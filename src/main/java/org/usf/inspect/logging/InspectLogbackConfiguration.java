package org.usf.inspect.logging;

import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getILoggerFactory;
import static org.slf4j.LoggerFactory.getLogger;
import static org.usf.inspect.core.Constants.LOGBACK_ENABLED;
import static org.usf.inspect.core.Constants.REPORTER;
import static org.usf.inspect.core.Helper.formatLocation;
import static org.usf.inspect.core.ReportEvent.report;
import static org.usf.inspect.core.SessionContextManager.createLog;
import static org.usf.inspect.core.TraceHub.hub;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.usf.inspect.core.InspectConfiguration;
import org.usf.inspect.core.InspectCoreConfiguration;
import org.usf.inspect.core.ReportEvent;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Configuration
@ConditionalOnBean(InspectCoreConfiguration.class) //inspect enabled
@ConditionalOnClass(name = "ch.qos.logback.classic.Logger") //slf-api dependency present
@ConditionalOnProperty(name = LOGBACK_ENABLED, havingValue = "true", matchIfMissing = true)
public class InspectLogbackConfiguration {//Async !?
	
    InspectLogbackConfiguration(InspectConfiguration config) {
		var prp = config.getProperties().getMonitoring().getLogback();
		var lvl = Level.valueOf(prp.getLevel().name());
        var app = new InspectLogAppender(lvl);
        app.setContext((LoggerContext) getILoggerFactory());
        app.setName("INSPECT_APPENDER");
        app.start();
        ((Logger) getLogger(prp.getRootPackage())).addAppender(app);        
    }

	@RequiredArgsConstructor
	public static final class InspectLogAppender extends AppenderBase<ILoggingEvent> {
		
		private final Level level;

	    @Override
	    protected void append(ILoggingEvent event) {//TODO exclude REPORTER
	        if(event.getLevel().isGreaterOrEqual(level)) {
	        	try {//trace exception/stack !?
					var lvl = event.getLevel();
					var lct = locationFrom(event.getCallerData(), event.getLoggerName());
					hub().emitTrace(createLog(event.getInstant(), event.getThreadName(), lvl.toString(), event.getFormattedMessage(), lct));
	        	}
	        	catch (Exception e) {
	        		REPORTER.error("InspectLogAppender.append: ", e);
	        	}
	        }
	    }
	}
	
	static String locationFrom(StackTraceElement[] stack, String defaultLocation) {
		if(nonNull(stack) && stack.length>0) {
			var ste = stack[0];
			return formatLocation(ste.getClassName(), ste.getMethodName(), ste.getLineNumber());
		}
		return defaultLocation;
	}
}
