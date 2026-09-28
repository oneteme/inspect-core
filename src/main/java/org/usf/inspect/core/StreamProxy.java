package org.usf.inspect.core;

import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static java.util.stream.StreamSupport.stream;
import static org.usf.inspect.core.SessionContextManager.requireActiveContext;
import static org.usf.inspect.core.SessionPropagator.runInContext;
import static org.usf.inspect.core.SessionPropagator.supplyInContext;
import static org.usf.inspect.core.TraceHub.hub;

import java.util.Collection;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StreamProxy {

	@SafeVarargs
	public static <T> Stream<T> parallelStream(T... array) {
		return parallel(stream(array));
	}

	public static <T> Stream<T> parallelStream(Collection<T> c) {
		return parallel(c.stream());
	}

	public static <T> Stream<T> parallel(Stream<T> stream) {
		return trackStream(stream.parallel());
	}
	
	public static <T> Stream<T> trackStream(Stream<T> stream) {
		if(hub().isEnabled()){
			var ctx = requireActiveContext();
			if(nonNull(ctx)) {
				return stream(new ContextSpliterator<>(stream.spliterator(), ctx), stream.isParallel())
						.onClose(stream::close);
			}
		}
		return stream;
	}
	
	@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
	static final class ContextSpliterator<T> implements Spliterator<T> {
		
		@Delegate
	    private final Spliterator<T> delegate;
	    private final AbstractSessionUpdate ctx;

	    @Override 
	    public boolean tryAdvance(Consumer<? super T> action) {
		    //TODO check this forEachRemaining-> tryAdvance
	        return supplyInContext(()-> delegate.tryAdvance(action), ctx);
	    }
	    
	    @Override 
	    public void forEachRemaining(Consumer<? super T> action) {
	    	Consumer<? super T> wrap = o-> runInContext(()-> action.accept(o), ctx);
	    	delegate.forEachRemaining(wrap);
	    }
	    
	    @Override 
	    public Spliterator<T> trySplit() {
	        var s = delegate.trySplit();
	        return nonNull(s) ? new ContextSpliterator<>(s, ctx) : null;
	    }
	}
}
