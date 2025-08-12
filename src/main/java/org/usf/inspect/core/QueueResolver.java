package org.usf.inspect.core;

import static java.time.Instant.MIN;
import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.UnaryOperator;

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
public final class QueueResolver {

	@Getter
	private final int queueCapacity;
	private final boolean modifiable;
	private final ConcurrentLinkedSetQueue<EventTrace> queue;

	public void dequeue(int delay, QueueConsumer cons) {
		dequeue(q->{
			var mdf = new ArrayList<>(q);
			var mrk = delay < 0 ? MIN : now().minusSeconds(delay);
			var kpt = new LinkedHashSet<EventTrace>();
			var pnd = extractPendingTrace(mdf, mrk, kpt); // 0: takes all, -1: completed only, 
			var rtr = cons.accept(mdf, pnd); //may contains traces copy
			if(nonNull(rtr)) {
				kpt.addAll(rtr);
			}
			return kpt; // requeue kept & returned traces
		});
	}
	
	public void dequeue(UnaryOperator<Collection<EventTrace>> op) {
		Collection<EventTrace> set = queue.pop();
		try {
			set = op.apply(set);
		}
		catch (OutOfMemoryError e) {
			set = emptyList(); //do not add items back to the queue, may release memory
			log.error("out of memory error while queue processing, {} traces will be aborted", set.size());
			throw e;
		}
		finally {
			if(nonNull(set) && !set.isEmpty()) {
				queue.addAll(false, set); //go back to the queue (!overwriting)
			}
		}
	}

	int extractPendingTrace(List<EventTrace> queue, Instant mark, Collection<EventTrace> kept) {
		var n = new int[1];
		for(var it=queue.listIterator(); it.hasNext();) {
			if(it.next() instanceof CompletableMetric mtr) {
				mtr.runSynchronizedIfNotComplete(()-> {
					n[0]++;
					if(mtr.getStart().isBefore(mark)) {
						if(modifiable) {  //send copy, avoid dispatch same reference
							it.set(mtr.copy());
						} //else keep original trace
						log.trace("completable trace pending since {}, dequeued: {}", mtr.getStart(), mtr);
					}
					else {
						kept.add(mtr);
						it.remove();
						log.trace("completable trace pending since {}, kept in queue: {}", mtr.getStart(), mtr);
					}
				});
			}
		}
		return n[0];
	}

	static interface QueueConsumer {
	
		Collection<EventTrace> accept(List<EventTrace> traces, int pending);
	}
}
