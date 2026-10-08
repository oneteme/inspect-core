package org.usf.inspect.logging;

import static java.time.Instant.ofEpochMilli;
import static org.slf4j.LoggerFactory.getILoggerFactory;
import static org.slf4j.LoggerFactory.getLogger;
import static org.usf.inspect.core.SessionContextManager.createLog;
import static org.usf.inspect.core.TraceHub.hub;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Configuration
@ConditionalOnClass(name = "ch.qos.logback.classic.Logger")
@ConditionalOnProperty(prefix = "inspect.collector", name = "enabled", havingValue = "true")
public class LogbackModuleConfiguration {
	
	@PostConstruct
    public void configure() {
		var config = hub().getConfiguration().getMonitoring().getLogback();
		if(config.isEnabled()) {
			var lvl = Level.valueOf(config.getLevel().name());
	        var app = new InspectLogAppender(lvl);
	        app.setContext((LoggerContext) getILoggerFactory());
	        app.setName("INSPECT_APPENDER");
	        app.start();

	        ((Logger) getLogger(config.getRootPackage())).addAppender(app);
		}
    }

	@RequiredArgsConstructor
	public final class InspectLogAppender extends AppenderBase<ILoggingEvent> {
		
		private final Level level;

	    @Override
	    protected void append(ILoggingEvent event) {
	        if(event.getLevel().isGreaterOrEqual(level)) {
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
