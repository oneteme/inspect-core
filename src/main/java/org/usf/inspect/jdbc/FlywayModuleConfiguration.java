package org.usf.inspect.jdbc;

import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static org.usf.inspect.core.ExecutionMonitor.exec;
import static org.usf.inspect.core.LocalRequest.formatLocation;
import static org.usf.inspect.core.LocalRequestType.EXEC;
import static org.usf.inspect.core.SessionManager.localRequestHandler;
import static org.usf.inspect.jdbc.DataSourceWrapper.wrap;

import org.flywaydb.core.Flyway;
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
		return fly-> exec(fly::migrate, localRequestHandler(EXEC, 
				()-> "migration",
				()-> scriptLocation(fly),
				()-> fly.getConfiguration().getUser()));
	}
	
	static String scriptLocation(Flyway fly) {
		return nonNull(fly.getConfiguration().getLocations())
				? stream(fly.getConfiguration().getLocations()).map(Object::toString).collect(joining(","))
				: formatLocation(Flyway.class.getName(), "migrate");
	}
}