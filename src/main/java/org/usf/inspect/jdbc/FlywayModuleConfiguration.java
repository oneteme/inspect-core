package org.usf.inspect.jdbc;

import static org.usf.inspect.core.ExecutionMonitor.exec;
import static org.usf.inspect.jdbc.DataSourceWrapper.wrap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

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
	public FlywayMigrationStrategy flywayMigrationStrategy() {
		var mnt = new FlywayMigrationMonitor();
		return fly-> exec(fly::migrate, mnt.migrationHandler(fly));
	}
}