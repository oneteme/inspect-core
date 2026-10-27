package org.usf.inspect.slf4j;

import static ch.qos.logback.classic.Level.WARN;
import static ch.qos.logback.core.spi.FilterReply.DENY;
import static ch.qos.logback.core.spi.FilterReply.NEUTRAL;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Function.identity;
import static org.slf4j.LoggerFactory.getILoggerFactory;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import org.usf.inspect.ReportEvent;
import org.usf.inspect.StackTraceRow;
import org.usf.inspect.core.TraceHub;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public class InspectLogReporter extends AppenderBase<ILoggingEvent> {
	
	private static final Function<StackTraceElementProxy, StackTraceElement> TO_STACK_TRACE_ELEMENT = StackTraceElementProxy::getStackTraceElement;
	private static final Function<StackTraceElement, StackTraceElement> TO_STACK_TRACE_ELEMENT_IDENTITY = identity();
	
	private static final AtomicInteger counter = new AtomicInteger(0);
	private static final String[] EMPTY_REPORT = new String[2];

	private final TraceHub hub;
	private final boolean errorStackTrace;
	private final boolean runtimeStackTrace;
	
    @Override
    protected void append(ILoggingEvent event) {
    	try {
    		hub.emitTrace(createReportEvent(event));
    	}
    	catch (Exception e) {
    		//don't log to avoid infinite loop
    	}
    }
    
    protected ReportEvent createReportEvent(ILoggingEvent event) {
		String exp = null;
		StackTraceRow[] stc = null;
		if(nonNull(event.getThrowableProxy())) {
			exp = event.getThrowableProxy().getClassName() + ": " + event.getThrowableProxy().getMessage();
			if(errorStackTrace) {
				stc = toStackTraceRow(event.getThrowableProxy().getStackTraceElementProxyArray(), TO_STACK_TRACE_ELEMENT);
			}
		}
		if(runtimeStackTrace && isNull(stc)) {
			stc = toStackTraceRow(event.getCallerData(), TO_STACK_TRACE_ELEMENT_IDENTITY);
		}
		var arr = splitReportMessage(event.getFormattedMessage());
		return new ReportEvent(event.getInstant(), event.getThreadName(), arr[0], arr[1], exp, stc);
    }
    
    static String[] splitReportMessage(String msg) {
    	if(isNull(msg)) {
    		return EMPTY_REPORT;
    	}
    	var pos = msg.indexOf(':');
    	return pos > 0 
    			? new String[] {msg.substring(0, pos), msg.substring(pos+1).trim()} 
    			: new String[] {null, msg};
    }
	
    static <T> StackTraceRow[] toStackTraceRow(T[] stack, Function<T, StackTraceElement> fn) {
		if(nonNull(stack) && stack.length > 0) {
			var stc = new StackTraceRow[stack.length];
			for(var i=0; i<stack.length; i++) {
				var e = fn.apply(stack[i]);
				stc[i] = new StackTraceRow(e.getClassName(), e.getMethodName(), e.getLineNumber());
			}
			return stc;
		}
		return null;
	}
    
    public static Logger configureLogger(String loggerName, boolean additive, Supplier<InspectLogReporter> fn) {
    	var appn = fn.get();
        appn.setContext((LoggerContext) getILoggerFactory());
        appn.setName("INSPECT_REPORTER_" + counter.incrementAndGet());
        appn.start();
        appn.addFilter(new Filter<ILoggingEvent>() {
			@Override //double check (REPORTER.isAdditive=false)
			public FilterReply decide(ILoggingEvent event) { 
				return event.getLevel().isGreaterOrEqual(WARN) ? NEUTRAL : DENY;
			}
		});
        var root = configureLogger(loggerName, additive);
        root.addAppender(appn);
        return root;
    }

    public static Logger configureLogger(String loggerName, boolean additive) {
        var root = (Logger) getLogger(loggerName);
        root.setAdditive(additive);
        return root;
    }
}