package org.usf.traceapi.core;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;

import java.security.Principal;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 
 * @author u$f
 *
 */
@Configuration
@EnableConfigurationProperties(TraceConfig.class)
@ConditionalOnProperty(prefix = "api.tracing", name = "enabled", havingValue = "true")
public class TraceConfiguration implements WebMvcConfigurer  {
	
	static final ThreadLocal<IncomingRequest> localTrace = new InheritableThreadLocal<>();
	static final Supplier<String> idProvider = ()-> randomUUID().toString();
	
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
    	registry.addInterceptor(new IncomingRequestInterceptor());
    }
	
    @Bean
    public IncomingRequestFilter incomingRequestFilter(TraceConfig config) {
    	
    	ClientProvider cp = req-> ofNullable(req.getUserPrincipal())
        		.map(Principal::getName)
        		.orElse(null);
    	TraceSender ts = config.getUrl().isBlank() 
        		? res-> {} 
        		: new RemoteTraceSender(config);
    	return new IncomingRequestFilter(cp, ts, config.getApplication());
    }

    @Bean
    public OutcomingRequestInterceptor outcomingRequestInterceptor() {
        return new OutcomingRequestInterceptor();
    }

    @Bean
    public BeanPostProcessor dataSourceWrapper() {
    	return new BeanPostProcessor() {
    		@Override
    		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
	            return bean instanceof DataSource 
	            		? new DataSourceWrapper((DataSource) bean) 
	            		: bean;
    		}
		};
    }
    
}
