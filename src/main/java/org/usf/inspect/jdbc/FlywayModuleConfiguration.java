package org.usf.inspect.jdbc;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@Configuration
@ConditionalOnClass(name="org.flywaydb.core.api.configuration.FluentConfiguration")
@ConditionalOnProperty(prefix = "inspect.collector", name = "enabled", havingValue = "true")
public class FlywayModuleConfiguration {
    
    @Bean
    @DependsOn("inspectContext") //ensure inspectContext is loaded first
    FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
    	log.debug("loading 'flywayConfigurationCustomizer' bean ..");
    	return conf-> {
    		var ds = conf.getDataSource();
    		if(ds.getClass() != DataSourceWrapper.class) {
		    	log.info("wrapping flyway DataSource '{}' ..", ds.getClass());
    			conf.dataSource(new DataSourceWrapper(ds));
    		}
    	};
    }
}
