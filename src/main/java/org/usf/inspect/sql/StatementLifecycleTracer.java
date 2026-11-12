package org.usf.inspect.sql;

import static java.util.Arrays.copyOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.DatabaseAction.BATCH;
import static org.usf.inspect.DatabaseAction.EXECUTE;
import static org.usf.inspect.DatabaseAction.STATEMENT;
import static org.usf.inspect.DatabaseCommand.SQL;
import static org.usf.inspect.DatabaseCommand.extractCommand;
import static org.usf.inspect.core.Constants.REPORTER;
import static org.usf.inspect.core.TraceHub.hub;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.usf.inspect.DatabaseCommand;
import org.usf.inspect.DatabaseRequestStage;
import org.usf.inspect.StagePayload;
import org.usf.inspect.core.DualEventTracer.StageBuilder;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public class StatementLifecycleTracer {
	
	private static final Function<Integer, long[]> TO_INTEGER_ARRAY = n-> new long[] {n};
	private static final Function<Long, long[]> TO_LONG_ARRAY = v-> new long[] {v};
	
	@Getter
	private final DatabaseConnectionLifecycleTracer connectionTracer;
	private final boolean prepared; //important 

	private DatabaseCommand mainCommand;
	private DatabaseRequestStage lastExec; // hold last EXECUTE stage, to update rows count for getMoreResults
	private BatchStageBuilder batchStageBuilder;

	public ExecutionListener<Object> statementStageListener(String sql) {
		return connectionTracer.stageListener((s,e,o,t)->{
			if(nonNull(sql)) {
				mainCommand = extractCommand(sql);
			} // o may be null, if execution failed
			return connectionTracer.createStage(s, e, STATEMENT, null, (long[])null);
		});
	}

	public ExecutionListener<Void> addBatchStageListener(String sql) {
		if(isNull(batchStageBuilder)) {
			batchStageBuilder = new BatchStageBuilder(); 
		}
		return connectionTracer.stageListener(isNull(sql) ? batchStageBuilder : (s,e,o,t)->{
			mainCommand = mergeCommand(mainCommand, extractCommand(sql));
			return batchStageBuilder.newStage(s, e, o, t);
		});
	}
	
	public ExecutionListener<ResultSet> executeQueryStageListener(String sql) {
		return executeStageListener(sql, null); // no count 
	}

	public ExecutionListener<Boolean> executeStageListener(String sql) {
		return executeStageListener(sql, null); //-1 if select, cannot call  getUpdateCount
	}

	public ExecutionListener<Integer> executeUpdateStageListener(String sql) {
		return executeStageListener(sql, TO_INTEGER_ARRAY);
	}

	public ExecutionListener<Long> executeLargeUpdateStageListener(String sql) {
		return executeStageListener(sql, TO_LONG_ARRAY);
	}

	public ExecutionListener<int[]> executeBatchStageListener(){
		emitBatchStage(); //before batch execute
		return executeStageListener(null, arr-> {
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

	public ExecutionListener<long[]> executeLargeBatchStageListener() {
		emitBatchStage(); //before batch execute
		return executeStageListener(null, arr-> {
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
	
	void emitBatchStage() { //wait for last addBatch
		if(nonNull(batchStageBuilder)) { //batch & largeBatch
			hub().emitTrace(batchStageBuilder.getStage());
			batchStageBuilder = null;
		}
		else {
			REPORTER.warn("DatabaseConnectionLifecycleTracer.emitBatchStage: empty batch or already traced");
		}
	}

	private <T> ExecutionListener<T> executeStageListener(String sql, Function<T, long[]> countFn) {
		return connectionTracer.stageListener((s,e,o,t)->{
			if(nonNull(sql)) {
				mainCommand = mergeCommand(mainCommand, extractCommand(sql));
			} // o may be null, if execution failed
			var count = nonNull(countFn) && nonNull(o) ? countFn.apply(o) : null;
			lastExec = connectionTracer.createStage(s, e, EXECUTE, mainCommand, count);
			if(!prepared) {
				mainCommand = null;
			} //else, mainCommand is set by prepareStatement
			return lastExec;
		});
	}

	public void updateStageRowsCount(long rows) {
		if(rows > -1) {
			try { //lastStg may be already sent !!
				if(nonNull(lastExec) && EXECUTE.name().equals(lastExec.getName())) {
					var payload = lastExec.getPayload();
					if(isNull(payload)) {
						payload = new StagePayload(null, new long[]{rows});
						lastExec.setPayload(payload);
					}
					else {
						payload.setCount(appendLong(payload.getCount(), rows)); // getMoreResults
					}
				}
			}
			catch (Exception e) {
				REPORTER.warn("DatabaseConnectionLifecycleTracer.updateStageRowsCount: failed to update last stage rows count", e);
			}
		}
	}
	
	public void closeStatementStage() {
		//TODO 
	}
	
	static DatabaseCommand mergeCommand(DatabaseCommand main, DatabaseCommand cmd) {
		if(main == cmd || isNull(cmd)) {
			return main;
		}
		return isNull(main) ? cmd : SQL;
	}
	
	static long[] appendLong(long[]arr, long v) {
		if(nonNull(arr)) {
			var a = copyOf(arr, arr.length+1);
			a[arr.length] = v;
			return a;
		}
		return new long[] {v};
	}

	@Getter
	final class BatchStageBuilder implements StageBuilder<Void> {

		private DatabaseRequestStage stage;
		
		@Override
		public DatabaseRequestStage newStage(Instant start, Instant end, Void obj, Throwable thrw) {
			if(isNull(stage)) {
				stage = connectionTracer.createStage(start, end, BATCH, null, new long[] {1}); 
			}
			else {
				stage.getPayload().getCount()[0]++;
				stage.setEnd(end); //optim this	
			}	
			if(nonNull(thrw)) {
				batchStageBuilder = null; //reset batching trace
				return stage;
			}
			return null;
		}
	}
}
