package org.usf.inspect.jdbc;

import static java.util.Arrays.copyOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.Helper.log;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.submit;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.ExecutionMonitor.exec;
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
import java.util.stream.IntStream;

import org.usf.inspect.core.DatabaseRequest;
import org.usf.inspect.core.DatabaseRequestStage;
import org.usf.inspect.core.SafeCallable;
import org.usf.inspect.core.ExecutionMonitor.ExecutionMonitorListener;
import org.usf.inspect.core.SafeCallable.SafeRunnable;

/**
 * 
 * @author u$f
 *
 */
public class DatabaseStageTracker {
	
	private DatabaseRequest req;
	private List<SqlCommand> commands;
	private DatabaseRequestStage exec; // hold last exec stage
	private boolean prepared;
	
	public Connection getConnection(SafeCallable<Connection, SQLException> supplier, SQLFunction<Connection, ConnectionInfo> infoFn) throws SQLException {
		return call(supplier, connection(infoFn));
	}
	
	public String databaseInfo(SafeCallable<String, SQLException> supplier) throws SQLException {
		return call(supplier, databaseActionCreator(DATABASE));
	}

	public ResultSetWrapper schemaInfo(SafeCallable<ResultSet, SQLException> supplier) throws SQLException {
		return new ResultSetWrapper(call(supplier, databaseActionCreator(SCHEMA)), this);
	}
	
	public StatementWrapper statement(SafeCallable<Statement, SQLException> supplier) throws SQLException {
		prepared = false;
		commands = new ArrayList<>(1);
		return new StatementWrapper(call(supplier, databaseActionCreator(STATEMENT)), this);
	}
	
	public PreparedStatementWrapper preparedStatement(String sql, SafeCallable<PreparedStatement, SQLException> supplier) throws SQLException {
		prepared = true;
		commands = new ArrayList<>(1);
		return new PreparedStatementWrapper(call(supplier, (s,e,o,t)-> {
			req.append(newStage(STATEMENT, s, e, t));
			commands.add(isNull(sql) ? null : mainCommand(sql));
		}), this);
	}

	public DatabaseMetaData connectionMetadata(SafeCallable<DatabaseMetaData, SQLException> supplier) throws SQLException {
		return new DatabaseMetaDataWrapper(call(supplier, databaseActionCreator(METADATA)), this);
	}

	public ResultSetMetaData resultSetMetadata(SafeCallable<ResultSetMetaData, SQLException> supplier) throws SQLException {
		return call(supplier, databaseActionCreator(METADATA));
	}

	public void addBatch(String sql, SafeRunnable<SQLException> method) throws SQLException {
		exec(method, (s,e,v,t)-> submit(ses-> {
			var stg = req.getActions().isEmpty() ? null : req.getActions().getLast();
			if(nonNull(stg) && BATCH.name().equals(stg.getName())) { //safe++
				stg.setEnd(e); //optim this
				stg.getCount()[0]++;
				if(nonNull(t) && isNull(stg.getException())) {
					stg.setException(mainCauseException(t));
				}
			}
			else {
				stg = newStage(BATCH, s, e, t);
				stg.setCount(new long[] {1});
				req.append(stg);
			} //else illegal state
			if(nonNull(sql)) {
				commands.add(mainCommand(sql));
			}
		}));
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
		return call(supplier, (s,e,r,t)-> submit(ses-> {
			exec = newStage(EXECUTE, s, e, t);
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
		}));
	}

	public Savepoint savePoint(SafeCallable<Savepoint, SQLException> supplier) throws SQLException {
		return call(supplier, databaseActionCreator(SAVEPOINT));
	}
	
	public void commit(SafeRunnable<SQLException> method) throws SQLException {
		exec(method, databaseActionCreator(COMMIT));
	}
	
	public void rollback(SafeRunnable<SQLException> method) throws SQLException {
		exec(method, databaseActionCreator(ROLLBACK));
	}
	
	public void fetch(Instant start, SafeRunnable<SQLException> method, int n) throws SQLException {
		exec(method, (s,e,o,t)-> submit(ses-> {
			var stg = newStage(FETCH, start, e, t); // differed start
			stg.setCount(new long[] {n});
			req.append(stg);
		}));
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
	
	@Deprecated //change this => see getMoreResults + submit
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
		exec(method, (s,e,o,t)-> submit(ses-> {
			req.append(newStage(DISCONNECTION, s, e, t));
			req.setEnd(e);
		}));
	}
	
	ExecutionMonitorListener<Connection> connection(SQLFunction<Connection, ConnectionInfo> infoFn) throws SQLException {
		req = new DatabaseRequest();
		return (s,e,o,t)->{
			req.setThreadName(threadName());
			var info = infoFn.apply(o);  //broke connection dependency
			submit(ses-> {
				req.setStart(s);
				if(nonNull(t)) {
					req.setEnd(e);
				}
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
				req.append(newStage(CONNECTION, s, e, t));
				ses.append(req);
			});
		};
	}

	<T> ExecutionMonitorListener<T> databaseActionCreator(JDBCAction action) {
		return (s,e,o,t)-> submit(ses-> req.append(newStage(action, s, e, t)));
	}

	static DatabaseRequestStage newStage(JDBCAction action, Instant start, Instant end, Throwable t) {
		var stg = new DatabaseRequestStage();
		stg.setName(action.name());
		stg.setStart(start);
		stg.setEnd(end);
		if(nonNull(t)) {
			stg.setException(mainCauseException(t));
		}
		return stg;
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