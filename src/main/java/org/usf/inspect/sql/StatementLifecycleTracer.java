package org.usf.inspect.sql;

import static java.util.Arrays.copyOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.DatabaseAction.BATCH;
import static org.usf.inspect.DatabaseAction.EXECUTE;
import static org.usf.inspect.DatabaseAction.STATEMENT;
import static org.usf.inspect.DatabaseCommand.SQL;
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
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor
public class StatementLifecycleTracer {
	
	private static final Function<Integer, long[]> TO_LONG_ARRAY = n-> new long[] {n};
	private static final Function<Long, long[]> TO_ARRAY = v-> new long[] {v};
	
	@Getter
	private final DatabaseConnectionLifecycleTracer connectionTracer;
	private final boolean prepared; //important 

	private DatabaseCommand mainCommand;
	private DatabaseRequestStage executeStage; // hold last EXECUTE stage, to update rows count for getMoreResults
	private BatchStageBuilder batchStageBuilder;
	
	private boolean canUpdateResults;

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
		return connectionTracer.stageListener(false, isNull(sql) ? batchStageBuilder : (s,e,o,t)->{
			mainCommand = mergeCommand(mainCommand, extractCommand(sql)); // update main command for batch statement
			return batchStageBuilder.newStage(s, e, o, t); //update current stage 
		});
	}
	
	public ExecutionListener<ResultSet> executeQueryStageListener(String sql) {
		return executeStageListener(sql, null); // no count 
	}

	public ExecutionListener<Boolean> executeStageListener(String sql) {
		return executeStageListener(sql, null); //-1 if select, cannot call  getUpdateCount
	}

	public ExecutionListener<Integer> executeUpdateStageListener(String sql) {
		return executeStageListener(sql, TO_LONG_ARRAY);
	}

	public ExecutionListener<Long> executeLargeUpdateStageListener(String sql) {
		return executeStageListener(sql, TO_ARRAY);
	}

	public ExecutionListener<int[]> executeBatchStageListener(){
		emitBatchStage(); //before batch execute
		return executeStageListener(null, arr-> { //compact array if all values are the same, to avoid huge payload
			if(arr.length > 1 && arr[0] > -1) { //SUCCESS_NO_INFO(-2) / EXECUTE_FAILED(-3)
				var i=0;
				while(++i<arr.length && arr[i]==arr[0]);
				if(i==arr.length){
					return new long[] {i*(long)arr[0]}; // [n,n,n,..,n] => [nN]
				}
			}
			return IntStream.of(arr).mapToLong(v->v).toArray();
		});
	}

	public ExecutionListener<long[]> executeLargeBatchStageListener() {
		emitBatchStage();
		return executeStageListener(null, arr-> { //compact array if all values are the same, to avoid huge payload
			if(arr.length > 1 && arr[0] > -1) {  //SUCCESS_NO_INFO(-2) / EXECUTE_FAILED(-3)
				var i=0;
				while(++i<arr.length && arr[i]==arr[0]);
				if(i==arr.length){
					return new long[] {i*arr[0]}; // [n,n,n,..,n] => [nN]
				}
			}
			return arr;
		});
	}
	
	void emitBatchStage() {
		if(nonNull(batchStageBuilder)) { //just trace the batch stage, no need to check if update is active
			hub().emitTrace(batchStageBuilder.getStage());
			batchStageBuilder = null;
		}
		else {
			log.warn("batch stage is null, maybe no addBatch() was called before executeBatch()");
		}
	}

	private <T> ExecutionListener<T> executeStageListener(String sql, Function<T, long[]> countFn) {
		emitCurrentExecutionStage(); // multiple statement executes
		return connectionTracer.stageListener(false, (s,e,o,t)->{ //do not trace stage now
			canUpdateResults = isNull(countFn); //
			var count = nonNull(countFn) && nonNull(o) ? countFn.apply(o) : null;
			var cmd = nonNull(sql) ? extractCommand(sql) : mainCommand;
			executeStage = connectionTracer.createStage(s, e, EXECUTE, cmd, count);
			if(!prepared) {
				mainCommand = null; //reset after batch statement
			}
			return executeStage;
		});
	}
	
	public void emitCurrentExecutionStage() {
		if(nonNull(batchStageBuilder)) {
			log.warn("batch stage is not null, maybe no executeBatch() was called after addBatch()");
			emitBatchStage(); //connection may be closed before executeBatch() is called, so emit batch stage now
		}
		if(nonNull(executeStage)) {
			hub().emitTrace(executeStage);
			executeStage = null;
			canUpdateResults = false;
		}
	}
	
	public void updateStageRowsCount(long rows) {
		if(rows > -1 && canUpdateResults) {
			try {
				if(nonNull(executeStage)) {
					var payload = executeStage.getPayload();
					if(isNull(payload)) {
						payload = new StagePayload(null, new long[]{rows});
						executeStage.setPayload(payload);
					}
					else {
						payload.setCount(appendLong(payload.getCount(), rows)); // getMoreResults
					}
				}
			}
			catch (Exception e) {
				REPORTER.warn("StatementLifecycleTracer.updateStageRowsCount: failed to update last stage rows count", e);
			}
		}
		canUpdateResults = false;
	}
	
	public void onMoreResults(boolean more) {
		canUpdateResults = more; //next getMoreResults will update rows count
	}
	
	public static DatabaseCommand extractCommand(String sql) {
		try {
			return DatabaseCommand.extractCommand(sql);
		}
		catch (Exception e) {
			REPORTER.error("StatementLifecycleTracer.extractCommand: failed to parse command", e);
		}
		return SQL;
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
				stage.setEnd(end); //optimize this	
			}	
			if(nonNull(thrw)) {
				hub().emitTrace(stage); //emit stage now, because exception will be thrown and no more batch will be executed
				batchStageBuilder = null; //reset batching trace
			}
			return stage;
		}
	}
}
