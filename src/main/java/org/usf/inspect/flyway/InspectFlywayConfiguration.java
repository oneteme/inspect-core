package org.usf.inspect.flyway;

import static java.time.Clock.systemUTC;
import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static org.usf.inspect.core.Constants.FLYWAY_ENABLED;
import static org.usf.inspect.core.ExecutionTracer.forLocalRequest;
import static org.usf.inspect.core.Helper.formatLocation;
import static org.usf.inspect.core.InspectExecutor.exec;
import static org.usf.inspect.core.LocalRequestType.EXEC;
import static org.usf.inspect.jdbc.DataSourceWrapper.wrap;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.usf.inspect.core.InspectCoreConfiguration;

/**
 * 
 * @author u$f
 *
 */
@Configuration
@ConditionalOnBean(InspectCoreConfiguration.class) //inspect enabled
@ConditionalOnClass(name="org.flywaydb.core.api.configuration.FluentConfiguration") //flayway dependency present
@ConditionalOnProperty(name = FLYWAY_ENABLED, havingValue = "true", matchIfMissing = true)
public class InspectFlywayConfiguration {
	
	@Bean
	FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
		return conf-> conf.dataSource(wrap(conf.getDataSource(), "flywayDataSource"));
	}

	@Bean
	public FlywayMigrationStrategy flywayMigrationStrategy() {
		return fly-> exec(fly::migrate, forLocalRequest(systemUTC().instant(), sgn->{
			sgn.setType(EXEC.name());
			sgn.setName("FlywayMigration");
			sgn.setLocation(scriptLocation(fly));
			sgn.setUser(fly.getConfiguration().getUser());
		}));
	}
	
	static String scriptLocation(Flyway fly) {
		return nonNull(fly.getConfiguration().getLocations())
				? stream(fly.getConfiguration().getLocations()).map(Object::toString).collect(joining(","))
				: formatLocation(Flyway.class.getName(), "migrate");
	}
}