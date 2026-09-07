package org.usf.inspect.jdbc;

import static java.time.Clock.systemUTC;
import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static org.usf.inspect.core.ExecutionTracer.forLocalRequest;
import static org.usf.inspect.core.Helper.formatLocation;
import static org.usf.inspect.core.InspectExecutor.exec;
import static org.usf.inspect.core.LocalRequestType.EXEC;
import static org.usf.inspect.core.SessionContextManager.createLocalRequest;
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
	@DependsOn("inspectHub") //ensure inspectHub is loaded first
	FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
		return conf-> conf.dataSource(wrap(conf.getDataSource(), "flywayDataSource"));
	}

	@Bean
	public FlywayMigrationStrategy flywayMigrationStrategy() {
		return fly-> exec(fly::migrate, forLocalRequest(()->{
			var sgn = createLocalRequest(systemUTC().instant());
			sgn.setType(EXEC.name());
			sgn.setName("FlywayMigration");
			sgn.setLocation(scriptLocation(fly));
			sgn.setUser(fly.getConfiguration().getUser());
			return sgn;
		}));
	}
	
	static String scriptLocation(Flyway fly) {
		return nonNull(fly.getConfiguration().getLocations())
				? stream(fly.getConfiguration().getLocations()).map(Object::toString).collect(joining(","))
				: formatLocation(Flyway.class.getName(), "migrate");
	}
}