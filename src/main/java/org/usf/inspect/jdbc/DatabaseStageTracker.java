package org.usf.inspect.jdbc;

import static java.util.Arrays.copyOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.Helper.log;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.requestAppender;
import static org.usf.inspect.core.StageTracker.call;
import static org.usf.inspect.core.StageTracker.exec;
import static org.usf.inspect.jdbc.JDBCAction.BATCH;
import static org.usf.inspect.jdbc.JDBCAction.COMMIT;
import static org.usf.inspect.jdbc.JDBCAction.CONNECTION;
import static org.usf.inspect.jdbc.JDBCAction.DATABASE;
import static org.usf.inspect.jdbc.JDBCAction.DISCONNECTION;
import static org.usf.inspect.jdbc.JDBCAction.EXECUTE;
import static org.usf.inspect.jdbc.JDBCAction.FETCH;
import static org.usf.inspect.jdbc.JDBCAction.METADATA;
import static org.usf.inspect.jdbc.JDBCAction.ROLLBACK;
import static org.usf.inspect.jdbc.JDBCAction.SAVEPOINT;
import static org.usf.inspect.jdbc.JDBCAction.SCHEMA;
import static org.usf.inspect.jdbc.JDBCAction.STATEMENT;
import static org.usf.inspect.jdbc.SqlCommand.mainCommand;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.IntStream;

import org.usf.inspect.core.DatabaseRequest;
import org.usf.inspect.core.DatabaseRequestStage;
import org.usf.inspect.core.SafeCallable;
import org.usf.inspect.core.SafeCallable.SafeRunnable;
import org.usf.inspect.core.StageTracker.StageCreator;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DatabaseStageTracker {
	
	private final DatabaseRequest req;
	private List<SqlCommand> commands;
	private DatabaseRequestStage exec; // hold last exec stage
	private boolean prepared;
	
	public String databaseInfo(SafeCallable<String, SQLException> supplier) throws SQLException {
		return call(supplier, databaseActionCreator(DATABASE), req::append);
	}

	public ResultSetWrapper schemaInfo(SafeCallable<ResultSet, SQLException> supplier) throws SQLException {
		return new ResultSetWrapper(call(supplier, databaseActionCreator(SCHEMA), req::append), this);
	}
	
	public StatementWrapper statement(SafeCallable<Statement, SQLException> supplier) throws SQLException {
		prepared = false;
		commands = new ArrayList<>(1);
		return new StatementWrapper(call(supplier, databaseActionCreator(STATEMENT), req::append), this);
	}
	
	public PreparedStatementWrapper preparedStatement(String sql, SafeCallable<PreparedStatement, SQLException> supplier) throws SQLException {
		prepared = true;
		commands = new ArrayList<>(1);
		return new PreparedStatementWrapper(call(supplier, databaseActionCreator(STATEMENT), stg->{
			req.append(stg);
			commands.add(isNull(sql) ? null : mainCommand(sql));
		}), this);
	}

	public DatabaseMetaData connectionMetadata(SafeCallable<DatabaseMetaData, SQLException> supplier) throws SQLException {
		return new DatabaseMetaDataWrapper(call(supplier, databaseActionCreator(METADATA), req::append), this);
	}

	public ResultSetMetaData resultSetMetadata(SafeCallable<ResultSetMetaData, SQLException> supplier) throws SQLException {
		return call(supplier, databaseActionCreator(METADATA), req::append);
	}

	public void addBatch(String sql, SafeRunnable<SQLException> method) throws SQLException {
		exec(method, (s,e,v,t)->{
			var stg = req.getActions().isEmpty() ? null : req.getActions().getLast();
			if(isNull(stg) || !BATCH.name().equals(stg.getName())) {
				stg = databaseActionCreator(BATCH).create(s, e, v, t);
				stg.setCount(new long[] {1});
				req.append(stg);
			}
			else {
				stg.setEnd(e); 
				stg.getCount()[0]++;
				if(nonNull(t) && isNull(stg.getException())) {
					stg.setException(mainCauseException(t));
				} //else illegal state
			}
			if(nonNull(sql)) {
				commands.add(mainCommand(sql));
			}
		});
	}

	public ResultSetWrapper resultSet(SafeCallable<ResultSet, SQLException> supplier) throws SQLException {
		return new ResultSetWrapper(supplier.call(), this);  // no need to trace this
	}

	public ResultSetWrapper executeQuery(String sql, SafeCallable<ResultSet, SQLException> supplier) throws SQLException {
		return new ResultSetWrapper(execute(sql, supplier, null), this); // no count 
	}

	public boolean execute(String sql, SafeCallable<Boolean, SQLException> supplier) throws SQLException {
		return execute(sql, supplier, b-> null);
	}
	
	public int executeUpdate(String sql, SafeCallable<Integer, SQLException> supplier) throws SQLException {
		return execute(sql, supplier, n-> new long[] {n});
	}
	
	public long executeLargeUpdate(String sql, SafeCallable<Long, SQLException> supplier) throws SQLException {
		return execute(sql, supplier, n-> new long[] {n});
	}

	public int[] executeBatch(SafeCallable<int[], SQLException> supplier) throws SQLException {
		return execute(null, supplier, arr-> {
			if(arr.length > 1) { 
				var i=0;
				while(++i<arr.length && arr[i]==arr[0]);
				if(i==arr.length){
					return new long[] {i*arr[0]}; // [n,n,n,..,n] => [nN]
				}
			}
			return IntStream.of(arr).mapToLong(v->v).toArray();
		});
	}
	
	public long[] executeLargeBatch(SafeCallable<long[], SQLException> supplier) throws SQLException {
		return execute(null, supplier, arr-> {
			if(arr.length > 1) {
				var i=0;
				while(++i<arr.length && arr[i]==arr[0]);
				if(i==arr.length){
					return new long[] {i*arr[0]}; // [n,n,n,..,n] => [nN]
				}
			}
			return arr;
		});
	}

	private <T> T execute(String sql, SafeCallable<T, SQLException> supplier, Function<T, long[]> countFn) throws SQLException {
		return call(supplier, (s,e,r,t)->{
			exec = databaseActionCreator(EXECUTE).create(s, e, r, t); //preserve last exec stage
			req.append(exec);
			if(nonNull(r) && nonNull(countFn)) {
				exec.setCount(countFn.apply(r));
			}
			if(nonNull(sql)) { //statement
				commands.add(mainCommand(sql));
			}
			exec.setCommands(commands.toArray(SqlCommand[]::new));
			if(nonNull(sql) || !prepared) { //statement.batch: sql is null
				commands = new ArrayList<>();
			}
		});
	}

	public Savepoint savePoint(SafeCallable<Savepoint, SQLException> supplier) throws SQLException {
		return call(supplier, databaseActionCreator(SAVEPOINT), req::append);
	}
	
	public void commit(SafeRunnable<SQLException> method) throws SQLException {
		exec(method, databaseActionCreator(COMMIT), req::append);
	}
	
	public void rollback(SafeRunnable<SQLException> method) throws SQLException {
		exec(method, databaseActionCreator(ROLLBACK), req::append);
	}
	
	public void fetch(Instant start, SafeRunnable<SQLException> method, int n) throws SQLException {
		exec(method, databaseActionCreator(FETCH), stg-> {
			stg.setStart(start); // differed start
			stg.setCount(new long[] {n});
			req.append(stg);
		});
	}
	
	public int updateCount(SafeCallable<Integer, SQLException> supplier) throws SQLException {
		var n = supplier.call();
		updateCount(n);
		return n;
	}

	public long largeUpdateCount(SafeCallable<Long, SQLException> supplier) throws SQLException {
		var n = supplier.call();
		updateCount(n);
		return n;
	}	
	
	private void updateCount(long n) {
		if(n > -1 && nonNull(exec)) {
			try { //safe
				var arr = exec.getCount();
				exec.setCount(isNull(arr) ? new long[] {n} : appendLong(arr, n)); // getMoreResults
			}
			catch (Exception e) {
				log.warn("cannot collect updateCount metrics => [{}]:{}", e.getClass().getSimpleName(), e.getMessage());
			}
		}
	}

	public void disconnection(SafeRunnable<SQLException> method) throws SQLException {
		exec(method, databaseActionCreator(DISCONNECTION), stg->{
			req.append(stg);
			req.setEnd(stg.getEnd());
		});
	}

	static StageCreator<Object, DatabaseRequestStage> databaseActionCreator(JDBCAction action) {
		return (s,e,o,t)->{
			var stg = new DatabaseRequestStage();
			stg.setName(action.name());
			stg.setStart(s);
			stg.setEnd(e);
			if(nonNull(t)) {
				stg.setException(mainCauseException(t));
			}
			return stg;
		};
	}
	
	public static ConnectionWrapper connection(SafeCallable<Connection, SQLException> supplier, SQLFunction<Connection, ConnectionInfo> infoFn) throws SQLException {
		var req = new DatabaseRequest();
		var trk = new DatabaseStageTracker(req);
		var cnx = call(supplier, (s,e,cn,t)->{
			req.setStart(s);
			req.setThreadName(threadName());
			if(nonNull(t)) {
				req.setEnd(e);
			}
			var info = infoFn.apply(cn); // cn can be null
			if(nonNull(info)) {
				req.setSchema(info.schema());
				req.setUser(info.user()); //TD different user !
				req.setScheme(info.scheme());
				req.setHost(info.host());
				req.setPort(info.port());
				req.setName(info.name()); //getCatalog
				req.setProductName(info.productName());
				req.setProductVersion(info.productVersion());
				req.setDriverVersion(info.driverVersion());
			}
			req.setActions(new ArrayList<>(4)); //cnx, stmt, exec, dec
			req.append(databaseActionCreator(CONNECTION).create(s, e, cn, t));
			return req;
		}, requestAppender());
		return new ConnectionWrapper(cnx, trk);
	}
	
	static long[] appendLong(long[]arr, long v) {
		var a = copyOf(arr, arr.length+1);
		a[arr.length] = v;
		return a;
	}
	
	public interface SQLFunction<T,R> {
		
		R apply(T o) throws SQLException;
	}
}