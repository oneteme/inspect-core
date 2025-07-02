package org.usf.inspect.jdbc;

import static java.util.Arrays.copyOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.ExecutionMonitor.exec;
import static org.usf.inspect.core.Helper.log;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.startDatabaseRequest;
import static org.usf.inspect.core.MetricsBroadcast.emit;
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
import org.usf.inspect.core.ExecutionMonitor.ExecutionMonitorListener;
import org.usf.inspect.core.SafeCallable;
import org.usf.inspect.core.SafeCallable.SafeRunnable;

/**
 * 
 * @author u$f
 *
 */
public class DatabaseStageTracker {
	
	private DatabaseRequest req;
	private List<SqlCommand> commands;
	private DatabaseRequestStage lastStage; // hold last exec stage
	private boolean prepared;
	
	public Connection getConnection(SafeCallable<Connection, SQLException> supplier, SQLFunction<Connection, ConnectionInfo> infoFn) throws SQLException {
		return new ConnectionWrapper(call(supplier, jdbcRequestListener(infoFn)), this);
	}
	
	public String databaseInfo(SafeCallable<String, SQLException> supplier) throws SQLException {
		return call(supplier, jdbcStageListener(DATABASE));
	}

	public ResultSetWrapper schemaInfo(SafeCallable<ResultSet, SQLException> supplier) throws SQLException {
		return new ResultSetWrapper(call(supplier, jdbcStageListener(SCHEMA)), this);
	}
	
	public StatementWrapper statement(SafeCallable<Statement, SQLException> supplier) throws SQLException {
		prepared = false;
		commands = new ArrayList<>(1);
		return new StatementWrapper(call(supplier, jdbcStageListener(STATEMENT)), this);
	}
	
	public PreparedStatementWrapper preparedStatement(String sql, SafeCallable<PreparedStatement, SQLException> supplier) throws SQLException {
		prepared = true;
		commands = new ArrayList<>(1);
		return new PreparedStatementWrapper(call(supplier, (s,e,o,t)-> {
			submitStage(jdbcStage(STATEMENT, s, e, t, null));
			commands.add(isNull(sql) ? null : mainCommand(sql));
		}), this);
	}

	public DatabaseMetaData connectionMetadata(SafeCallable<DatabaseMetaData, SQLException> supplier) throws SQLException {
		return new DatabaseMetaDataWrapper(call(supplier, jdbcStageListener(METADATA)), this);
	}

	public ResultSetMetaData resultSetMetadata(SafeCallable<ResultSetMetaData, SQLException> supplier) throws SQLException {
		return call(supplier, jdbcStageListener(METADATA));
	}

	public void addBatch(String sql, SafeRunnable<SQLException> method) throws SQLException {
		exec(method, (s,e,v,t)-> {
			if(isLastStage(BATCH)) { //safe++
				lastStage.setEnd(e); //optim this
				lastStage.getCount()[0]++;
				if(nonNull(t)) {
					lastStage.setException(mainCauseException(t)); //may overwrite previous
				}
			}
			else {
				submitStage(jdbcStage(BATCH, s, e, t, new long[] {1}));
			}
			if(nonNull(sql)) { //statement
				commands.add(mainCommand(sql));
			}
		});
	}

	public ResultSetWrapper resultSet(SafeCallable<ResultSet, SQLException> supplier) throws SQLException {
		return new ResultSetWrapper(supplier.call(), this);  // no need to trace this
	}

	public ResultSetWrapper executeQuery(String sql, SafeCallable<ResultSet, SQLException> supplier) throws SQLException {
		return new ResultSetWrapper(execute(sql, supplier, rs-> null), this); // no count 
	}

	public boolean execute(String sql, SafeCallable<Boolean, SQLException> supplier, Statement st) throws SQLException {
		return execute(sql, supplier, b-> new long[] {getUpdateCount(st)}); //-1 if select
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
		return call(supplier, (s,e,o,t)-> {
			var stg = jdbcStage(EXECUTE, s, e, t, nonNull(o) ? countFn.apply(o) : null); // o may be null, if execution failed
			if(nonNull(sql)) { //statement
				commands.add(mainCommand(sql));
			}
			stg.setCommands(commands.toArray(SqlCommand[]::new));
			if(!prepared) { //statement.batch: sql is null
				commands = new ArrayList<>();
			}
			submitStage(stg);
		});
	}

	public Savepoint savePoint(SafeCallable<Savepoint, SQLException> supplier) throws SQLException {
		return call(supplier, jdbcStageListener(SAVEPOINT));
	}
	
	public void commit(SafeRunnable<SQLException> method) throws SQLException {
		exec(method, jdbcStageListener(COMMIT));
	}
	
	public void rollback(SafeRunnable<SQLException> method) throws SQLException {
		exec(method, jdbcStageListener(ROLLBACK));
	}
	
	public void fetch(Instant start, SafeRunnable<SQLException> method, int n) throws SQLException {
		exec(method, (s,e,o,t)-> //differed start 
			submitStage(jdbcStage(FETCH, start, e, t, new long[] {n})));
	}

	/**
     * <P>There are no more results when the following is true:
     * <PRE>{@code
     *     ((stmt.getMoreResults() == false) && (stmt.getUpdateCount() == -1))
     * }</PRE>
	 */
    public boolean getMoreResults(Statement st) throws SQLException {
    	var more = st.getMoreResults();
    	if(isLastStage(EXECUTE)) { //safe++
    		try { //safe
            	var rows = getUpdateCount(st);
            	if(rows > -1) {
    				var arr = lastStage.getCount();
    				lastStage.setCount(isNull(arr) ? new long[] {rows} : appendLong(arr, rows)); // getMoreResults
    			}
    		}
    		catch (Exception e) {
    			log.warn("cannot collect updateCount metrics => [{}]:{}", e.getClass().getSimpleName(), e.getMessage());
    		}
    	}
    	return more;
    }
    
    long getUpdateCount(Statement st) {
    	try { //safe
        	var rows = st.getLargeUpdateCount();
        	if(rows == -1) {
        		rows = st.getUpdateCount(); //check both
        	}
        	return rows;
		}
		catch (Exception e) {
			log.warn("cannot collect updateCount metrics => [{}]:{}", e.getClass().getSimpleName(), e.getMessage());
		}
    	return -1;
    }
	
	public void disconnection(SafeRunnable<SQLException> method) throws SQLException {
		exec(method, (s,e,o,t)-> {
			emit(jdbcStage(DISCONNECTION, s, e, t, null));
			req.lazy(()-> {
				req.setEnd(e);
				emit(req);
			});
		});
	}
	
	ExecutionMonitorListener<Connection> jdbcRequestListener(SQLFunction<Connection, ConnectionInfo> infoFn) {
		req = startDatabaseRequest();
		return (s,e,o,t)->{
			req.setThreadName(threadName());
			req.setStart(s);
			if(nonNull(t)) {
				req.setFailed(true);
				req.setEnd(e);
			}
			var info = infoFn.apply(o);  //broke connection dependency
			if(nonNull(info)) {
				req.setScheme(info.scheme());
				req.setHost(info.host());
				req.setPort(info.port());
				req.setName(info.name()); //getCatalog
				req.setSchema(info.schema());
				req.setUser(info.user()); //TD different user !
				req.setProductName(info.productName());
				req.setProductVersion(info.productVersion());
				req.setDriverVersion(info.driverVersion());
			}
			emit(jdbcStage(CONNECTION, s, e, t, null));
			emit(req);
		};
	}

	<T> ExecutionMonitorListener<T> jdbcStageListener(JDBCAction action) {
		return (s,e,o,t)-> submitStage(jdbcStage(action, s, e, t, null));
	}
	
	private void submitStage(DatabaseRequestStage stg) {
		if(nonNull(stg.getException())) {
			req.lazy(()-> req.setFailed(true));
		}
		emit(stg);
		this.lastStage = stg; //hold last stage
	}

	DatabaseRequestStage jdbcStage(JDBCAction action, Instant start, Instant end, Throwable t, long[] count) {
		var stg = req.createStage(action.name(), start, end, t);
		stg.setCount(count);
		return stg;
	}
	
	boolean isLastStage(JDBCAction stg) {
		return nonNull(lastStage) && stg.name().equals(lastStage.getName());
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