package org.usf.inspect.sql;

import static org.usf.inspect.core.Constants.JDBC_ENABLED;
import static org.usf.inspect.sql.DataSourceWrapper.wrap;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.usf.inspect.core.InspectCoreConfiguration;

import com.zaxxer.hikari.HikariDataSource;

/**
 * 
 * @author u$f
 *
 */
@Configuration
@ConditionalOnBean(InspectCoreConfiguration.class)
@ConditionalOnProperty(name = JDBC_ENABLED, havingValue = "true", matchIfMissing = true)
public class InspectDatasourceConfiguration {

	@ConditionalOnClass(HikariDataSource.class)
	static class InspectHikariDataSourceConfiguration {

	    @Bean
	    static BeanPostProcessor hikariDataSourceWrapper() {
	    	return new BeanPostProcessor() {
	    		@Override
	    		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		            return bean instanceof HikariDataSource ds 
		            		? wrap(ds, new SingleDatasourceMetadataProvider(ds.getJdbcUrl()), beanName)
		            		: bean;
	    		}
			};
	    }
	}

	@ConditionalOnMissingClass("com.zaxxer.hikari.HikariDataSource")
	static class InspectDefaultDataSourceConfiguration {

	    @Bean
	    static BeanPostProcessor defaultDataSourceWrapper() {
	    	return new BeanPostProcessor() {
	    		@Override
	    		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		            return bean instanceof DataSource ds 
		            		? wrap(ds, new SingleDatasourceMetadataProvider(), beanName)
		            		: bean;
	    		}
			};
	    }
	}
}
