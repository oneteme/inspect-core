package org.usf.inspect.ldap;

import static org.usf.inspect.core.Constants.LDAP_ENABLED;
import static org.usf.inspect.ldap.ContextSourceWrapper.wrap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.ContextSource;
import org.usf.inspect.core.InspectCoreConfiguration;

/**
 * 
 * @author u$f
 *
 */
@Configuration
@ConditionalOnBean(InspectCoreConfiguration.class) //inspect enabled
@ConditionalOnClass(name="org.springframework.ldap.core.ContextSource") //spring-ldap dependency present
@ConditionalOnProperty(name = LDAP_ENABLED, havingValue = "true", matchIfMissing = true)
public class InspectDirectoryConfiguration {
	
	@Bean
	BeanPostProcessor contextSourceWrapper() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				return bean instanceof ContextSource cs ? wrap(cs, beanName) : bean;
			}
		};
	}
}
