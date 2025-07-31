package org.usf.inspect.dir;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.ldap.core.ContextSource;
import org.usf.inspect.rest.RestRequestInterceptor;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@Configuration
@ConditionalOnClass(name="org.springframework.ldap.core.ContextSource")
@ConditionalOnProperty(prefix = "inspect.collector", name = "enabled", havingValue = "true")
public class DirectoryModuleConfiguration {
	
	@Bean
	@DependsOn("inspectContext") //ensure inspectContext is loaded first
	BeanPostProcessor dataSourceWrapper(RestRequestInterceptor interceptor) {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
				if(bean instanceof ContextSource cs) {
					log.debug("wrapping ContextSource '{}' bean ..", beanName);
					bean = new ContextSourceWrapper(cs);
				}
				return bean;
			}
		};
	}
}
