package org.usf.inspect.dir;

import static org.usf.inspect.core.BeanUtils.logWrappingBean;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.ldap.core.ContextSource;
import org.usf.inspect.rest.RestRequestInterceptor;

/**
 * 
 * @author u$f
 *
 */
@Configuration
@ConditionalOnClass(name="org.springframework.ldap.core.ContextSource")
@ConditionalOnProperty(prefix = "inspect.collector", name = "enabled", havingValue = "true")
public class DirectoryModuleConfiguration {
	
	@Bean
	@DependsOn("inspectContext") //ensure inspectContext is loaded first
	BeanPostProcessor contextSourceWrapper(RestRequestInterceptor interceptor) {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				if(bean instanceof ContextSource cs && bean.getClass() != ContextSourceWrapper.class) {
					logWrappingBean(beanName, bean.getClass());
					bean = new ContextSourceWrapper(cs);
				}
				return bean;
			}
		};
	}
}
