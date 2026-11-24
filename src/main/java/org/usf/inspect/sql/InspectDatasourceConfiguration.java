package org.usf.inspect.sql;

import static org.usf.inspect.core.Constants.JDBC_ENABLED;
import static org.usf.inspect.core.Constants.REPORTER;
import static org.usf.inspect.sql.DataSourceWrapper.wrap;

import java.util.Objects;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.usf.inspect.core.InspectCoreConfiguration;

import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@Configuration
@ConditionalOnBean(InspectCoreConfiguration.class)
@ConditionalOnProperty(name = JDBC_ENABLED, havingValue = "true", matchIfMissing = true)
class InspectDatasourceConfiguration {

    @Bean
    static BeanPostProcessor dataSourceWrapper(ObjectProvider<DatasourceUrlResolver> resolvers) {
    	return new BeanPostProcessor() {
    		@Override
    		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    			if(bean instanceof DataSource ds) {
    				var prv = resolvers.stream()
    						.map(r-> r.resolve(ds))
    						.filter(Objects::nonNull)
    						.map(SingleDatasourceMetadataProvider::new)
    						.findFirst()
    						.orElseGet(SingleDatasourceMetadataProvider::new);
		            return wrap(ds, prv, beanName);
    			}
    			return bean;
    		}
		};
    }
	
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(HikariDataSource.class)
	static class HikariSupport {
	    @Bean
	    DatasourceUrlResolver hikariUrlResolver() {
	        return ds-> {
	        	try {
					if(ds.isWrapperFor(HikariDataSource.class)) {
						return ds.unwrap(HikariDataSource.class).getJdbcUrl();
					}
				} catch (Exception e) {
					REPORTER.error("Failed to resolve datasource url for {}", ds, e);
				}
	        	return null;
	        };
	    }
	}
}
