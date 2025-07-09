package org.usf.inspect.jdbc;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
    	log.debug("loading 'flywayConfigurationCustomizer' bean ..");
    	return conf-> conf.dataSource(new DataSourceWrapper(conf.getDataSource()));
    }
}
