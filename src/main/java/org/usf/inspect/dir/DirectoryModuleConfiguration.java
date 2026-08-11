package org.usf.inspect.dir;

import static org.usf.inspect.dir.ContextSourceWrapper.wrap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.ldap.core.ContextSource;

/**
 * Registers LDAP context source wrapping when inspection collection is enabled.
 */
@Configuration
@ConditionalOnClass(name="org.springframework.ldap.core.ContextSource")
@ConditionalOnProperty(prefix = "inspect.collector", name = "enabled", havingValue = "true")
public class DirectoryModuleConfiguration {
	
	@Bean
	@DependsOn("inspectHub") //ensure inspectHub is loaded first
	BeanPostProcessor contextSourceWrapper() {
		return new BeanPostProcessor() {
			/**
			 * Wraps initialized {@link ContextSource} beans with monitoring support.
			 *
			 * @param bean the initialized bean instance
			 * @param beanName the Spring bean name
			 * @return the wrapped context source or the original bean
			 * @throws BeansException if post-processing fails
			 */
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				return bean instanceof ContextSource cs ? wrap(cs, beanName) : bean;
			}
		};
	}
}
