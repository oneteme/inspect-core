package org.usf.inspect.jdbc;

import static java.time.Instant.now;
import static org.flywaydb.core.api.callback.Event.AFTER_MIGRATE;
import static org.flywaydb.core.api.callback.Event.BEFORE_MIGRATE;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.LocalRequestType.EXEC;
import static org.usf.inspect.core.SessionManager.createLocalRequest;
import static org.usf.inspect.jdbc.DataSourceWrapper.wrap;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.BaseCallback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.usf.inspect.core.LocalRequest;

/**
 * 
 * @author u$f
 *
 */
@Configuration
@ConditionalOnClass(name="org.flywaydb.core.api.configuration.FluentConfiguration")
@ConditionalOnProperty(prefix = "inspect.collector", name = "enabled", havingValue = "true")
public class FlywayModuleConfiguration {
    
    @Bean
    @DependsOn("inspectContext") //ensure inspectContext is loaded first
    FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
    	return conf-> wrap(conf.getDataSource(), "flywayDataSource");
    }
    
    @Bean
    @DependsOn("inspectContext") //ensure inspectContext is loaded first
    public BaseCallback baseCallback() {
    	
    	return new BaseCallback() {

    		private final LocalRequest req = createLocalRequest();
    		
    		@Override
			public void handle(Event event, Context context) {
    			if (event == BEFORE_MIGRATE) {
    				call(()->{
    					req.setStart(now());
    					req.setThreadName(threadName());
    					req.setType(EXEC.name());
    					req.setName("migration");
    					req.setLocation(Flyway.class.getName() + ".migrate()");
    					req.setUser(context.getConfiguration().getUser());
    					return req;
    				});
    	        } else if (event == AFTER_MIGRATE) {
    	        	call(()->{
    					req.setEnd(now());
    					return req;
    				});
    	        }
			}
		};
    }
}
