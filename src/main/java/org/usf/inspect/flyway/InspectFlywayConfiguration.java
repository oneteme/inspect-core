package org.usf.inspect.flyway;

import static java.time.Clock.systemUTC;
import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static org.usf.inspect.Helper.formatLocation;
import static org.usf.inspect.LocalRequestType.EXEC;
import static org.usf.inspect.core.Constants.FLYWAY_ENABLED;
import static org.usf.inspect.core.Constants.REPORTER;
import static org.usf.inspect.core.ExecutionTracer.localRequestTracer;
import static org.usf.inspect.core.InspectExecutor.exec;
import static org.usf.inspect.sql.DataSourceWrapper.wrap;

import java.sql.SQLException;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.usf.inspect.core.InspectCoreConfiguration;
import org.usf.inspect.sql.DataSourceWrapper;
import org.usf.inspect.sql.SingleDatasourceMetadataProvider;

/**
 * 
 * @author u$f
 *
 */
@Configuration
@ConditionalOnBean(InspectCoreConfiguration.class) //inspect enabled
@ConditionalOnClass(name="org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer") //flyway dependency present
@ConditionalOnProperty(name = FLYWAY_ENABLED, havingValue = "true", matchIfMissing = true)
public class InspectFlywayConfiguration {
	
	@Bean
	FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
		return conf->{
			var ds = conf.getDataSource();
			try {
				if(nonNull(ds) && !ds.isWrapperFor(DataSourceWrapper.class)) {
					conf.dataSource(wrap(ds, new SingleDatasourceMetadataProvider(), "flywayDataSource"));
				}
			} catch (SQLException e) {
				REPORTER.error("cannot wrap flyway datasource", e);
			}
		};
	}

	@Bean
	FlywayMigrationStrategy flywayMigrationStrategy() {//TODO wrap default strategy or user defined strategy
		return fly-> exec(fly::migrate, localRequestTracer(systemUTC().instant(), EXEC, sgn->{
			sgn.setName("FlywayMigration");
			sgn.setLocation(scriptLocation(fly));
			sgn.setUser(fly.getConfiguration().getUser());
		}));
	}
	
	static String scriptLocation(Flyway fly) {
		return nonNull(fly.getConfiguration().getLocations())
				? stream(fly.getConfiguration().getLocations()).map(Object::toString).collect(joining(","))
				: formatLocation(FlywayMigrationStrategy.class.getName(), "migrate");
	}
}