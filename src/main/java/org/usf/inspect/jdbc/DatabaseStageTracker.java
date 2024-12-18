package org.usf.inspect.jdbc;

import static java.time.Instant.now;
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
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.IntStream;

import org.usf.inspect.core.DatabaseRequest;
import org.usf.inspect.core.DatabaseRequestStage;
import org.usf.inspect.core.SafeCallable;
import org.usf.inspect.core.SafeCallable.SafeRunnable;
import org.usf.inspect.core.StageTracker.StageConsumer;
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
	private DatabaseRequestStage exec; //hold last execution stage
	
	public String databaseInfo(SafeCallable<String, SQLException> supplier) throws SQLException {
		return call(supplier, databaseActionCreator(DATABASE), req::append);
	}

	public ResultSetWrapper schemaInfo(SafeCallable<ResultSet, SQLException> supplier) throws SQLException {
		return new ResultSetWrapper(call(supplier, databaseActionCreator(SCHEMA), req::append), this, now());
	}
	
	public StatementWrapper statement(SafeCallable<Statement, SQLException> supplier) throws SQLException {
		return new StatementWrapper(call(supplier, databaseActionCreator(STATEMENT), req::append), this);
	}
	
	public PreparedStatementWrapper preparedStatement(String sql, SafeCallable<PreparedStatement, SQLException> supplier) throws SQLException {
		return new PreparedStatementWrapper(call(supplier, databaseActionCreator(STATEMENT), req::append), this, sql); //parse command on exec
	}

	public DatabaseMetaData connectionMetadata(SafeCallable<DatabaseMetaData, SQLException> supplier) throws SQLException {
		return new DatabaseMetaDataWrapper(call(supplier, databaseActionCreator(METADATA), req::append), this);
	}

	public ResultSetMetaData resultSetMetadata(SafeCallable<ResultSetMetaData, SQLException> supplier) throws SQLException {
		return call(supplier, databaseActionCreator(METADATA), req::append);
	}

	public ResultSetWrapper resultSet(SafeCallable<ResultSet, SQLException> supplier) throws SQLException {
		return new ResultSetWrapper(supplier.call(), this, now());  // no need to trace this
	}

	public ResultSetWrapper executeQuery(String sql, SafeCallable<ResultSet, SQLException> supplier) throws SQLException {
		return new ResultSetWrapper(execute(sql, supplier, rs-> null), this, now()); // no count 
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

	public int[] executeBatch(String sql, SafeCallable<int[], SQLException> supplier) throws SQLException {
		return execute(sql, supplier, n-> IntStream.of(n).mapToLong(v-> v).toArray());
	}
	
	public long[] executeLargeBatch(String sql, SafeCallable<long[], SQLException> supplier) throws SQLException {
		return execute(sql, supplier, n-> n);
	}

	private <T> T execute(String sql, SafeCallable<T, SQLException> supplier, Function<T, long[]> countFn) throws SQLException {
		if(nonNull(sql)) {
			req.getCommands().add(mainCommand(sql));
		} //BATCH otherwise 
		return call(supplier, databaseActionCreator(EXECUTE, (a,r)->{
			if(nonNull(r)) { //fail
				a.setCount(countFn.apply(r));
			}
			exec = a; //!important
		}), req::append);
	}

	public Savepoint savePoint(SafeCallable<Savepoint, SQLException> supplier) throws SQLException {
		return call(supplier, databaseActionCreator(SAVEPOINT), req::append);
	}

	public void addBatch(String sql, SafeRunnable<SQLException> method) throws SQLException {
		if(nonNull(sql)) {
			req.getCommands().add(mainCommand(sql));
		} // PreparedStatement otherwise 
		exec(method, nonNull(sql) || req.getActions().isEmpty() || !BATCH.name().equals(last(req.getActions()).getName())
				? databaseActionCreator(BATCH, (a,v)-> a.setCount(new long[] {1})).then(req::append) //statement | first batch
				: updateLast(last(req.getActions())));
	}
	
	public void commit(SafeRunnable<SQLException> method) throws SQLException {
		exec(method, databaseActionCreator(COMMIT), req::append);
	}
	
	public void rollback(SafeRunnable<SQLException> method) throws SQLException {
		exec(method, databaseActionCreator(ROLLBACK), req::append);
	}
	
	public void fetch(Instant start, SafeRunnable<SQLException> method, int n) throws SQLException {
		exec(method, databaseActionCreator(FETCH, (a, v)-> {
			a.setStart(start); // differed start
			a.setCount(new long[] {n});
		}), req::append);
	}
	
	public int updateCount(SafeCallable<Integer, SQLException> supplier) throws SQLException {
		return updateCount(supplier, n-> n);
	}

	public long largeUpdateCount(SafeCallable<Long, SQLException> supplier) throws SQLException {
		return updateCount(supplier, n-> n);
	}	
	
	private <T> T updateCount(SafeCallable<T, SQLException> supplier, ToLongFunction<T> fn) throws SQLException {
		var res = supplier.call();
		var n = fn.applyAsLong(res);
		if(n > -1 && nonNull(exec)) {
			try { //safe
				var arr = exec.getCount();
				exec.setCount(isNull(arr) ? new long[] {n} : appendLong(arr, n)); // getMoreResults
			}
			catch (Exception e) {
				log.warn("cannot collect updateCount metrics => {}", e.getMessage());
			}
		}
		return res;
	}

	public void disconnection(SafeRunnable<SQLException> method) throws SQLException {
		exec(method, databaseActionCreator(DISCONNECTION), stg->{
			req.append(stg);
			req.setEnd(stg.getEnd());
		});
	}

	<T> StageConsumer<T> updateLast(DatabaseRequestStage stg) {
		return (s,e,o,t)->{
			stg.setEnd(e); 
			stg.getCount()[0]++;
			if(nonNull(t) && isNull(stg.getException())) {
				stg.setException(mainCauseException(t));
			} //else illegal state
		};
	}

	static StageCreator<Object, DatabaseRequestStage> databaseActionCreator(JDBCAction action) {
		return databaseActionCreator(action, null);
	}
	
	static <T> StageCreator<T, DatabaseRequestStage> databaseActionCreator(JDBCAction action, BiConsumer<DatabaseRequestStage, T> cons) {
		return (s,e,o,t)->{
			var stg = new DatabaseRequestStage();
			stg.setName(action.name());
			stg.setStart(s);
			stg.setEnd(e);
			if(nonNull(t)) {
				stg.setException(mainCauseException(t));
			}
			if(nonNull(cons)) {
				cons.accept(stg, o); //o is nullable
			}
			return stg;
		};
	}
	
	static long[] appendLong(long[]arr, long v) {
		var a = copyOf(arr, arr.length+1);
		a[arr.length] = v;
		return a;
	}
	
	private static <T> T last(List<T> list) { //!empty
		return list.get(list.size()-1);
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
			req.setCommands(new ArrayList<>(1));
			req.append(databaseActionCreator(CONNECTION).create(s, e, cn, t));
			return req;
		}, requestAppender());
		return new ConnectionWrapper(cnx, trk);
	}
	
	public interface SQLFunction<T,R> {
		
		R apply(T o) throws SQLException;
	}
}