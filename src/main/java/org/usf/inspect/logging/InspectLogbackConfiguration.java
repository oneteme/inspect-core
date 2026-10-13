package org.usf.inspect.logging;

import static java.time.Instant.ofEpochMilli;
import static org.slf4j.LoggerFactory.getILoggerFactory;
import static org.slf4j.LoggerFactory.getLogger;
import static org.usf.inspect.core.Constants.LOGBACK_ENABLED;
import static org.usf.inspect.core.SessionContextManager.createLog;
import static org.usf.inspect.core.TraceHub.hub;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.usf.inspect.core.InspectConfiguration;
import org.usf.inspect.core.InspectCoreConfiguration;

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
public class InspectLogbackConfiguration {
	
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
	public final class InspectLogAppender extends AppenderBase<ILoggingEvent> {
		
		private final Level level;

	    @Override
	    protected void append(ILoggingEvent event) {
	        if(event.getLevel().isGreaterOrEqual(level)) { //exclude self
	        	try {
					var lvl = event.getLevel().toString();
					var tms = ofEpochMilli(event.getTimeStamp());
					hub().emitTrace(createLog(tms, lvl, event.getFormattedMessage(), event.getLoggerName()));
	        	}
	        	catch (Exception e) {
	        		hub().emitReport("InspectLogAppender.append", e);
	        	}
	        }
	    }
	}
}
