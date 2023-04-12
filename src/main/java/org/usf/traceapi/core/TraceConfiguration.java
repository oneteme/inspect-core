package org.usf.traceapi.core;

import static java.util.Optional.ofNullable;

import java.security.Principal;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TraceConfiguration {

    @Bean("trFilter")
    public ApiTraceFilter requestTracer(ClientSupplier cs, TraceSender sender) {
        return new ApiTraceFilter(cs, sender);
    }

    @Bean("trInterceptor")
    public ApiTraceInjector requestInterceptor() {
        return new ApiTraceInjector();
    }

    @Bean("trDataSource")
    public BeanPostProcessor queryTracer() {
    	return new BeanPostProcessor() {
    		@Override
    		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
	            return bean instanceof DataSource 
	            		? new DataSourceTrace((DataSource) bean) 
	            		: bean;
    		}
		};
    }
    
    @Bean
    public ClientSupplier clientSupplier() {
        return req-> ofNullable(req.getUserPrincipal()).map(Principal::getName).orElse(null); //unknown
    }
    
    @Bean
    public TraceSender traceSender(
    		@Value("${tracing.server.url:}") String url,
    		@Value("${tracing.enabled:true}") boolean enabled,
    		@Value("${tracing.delay:5}") int delay,
    		@Value("${tracing.unit:SECONDS}") String unit) {
    	
        return !enabled || url.isBlank() 
        		? res-> {} 
        		: new RemoteTraceSender(url, delay, TimeUnit.valueOf(unit));
    }

}
