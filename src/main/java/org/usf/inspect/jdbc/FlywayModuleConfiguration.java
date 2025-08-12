package org.usf.inspect.jdbc;

import static org.usf.inspect.core.BeanUtils.logLoadingBean;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
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
    	logLoadingBean("flywayConfigurationCustomizer", FlywayConfigurationCustomizer.class);
    	return conf-> {
    		var ds = conf.getDataSource();
    		if(ds.getClass() != DataSourceWrapper.class) { //flyway may use the default datasource if its own is not set 
				logWrappingBean("flywayDataSource", ds.getClass());
    			conf.dataSource(new DataSourceWrapper(ds));
    		}
    	};
    }
}
