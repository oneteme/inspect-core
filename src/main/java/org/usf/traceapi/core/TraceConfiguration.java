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
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class TraceConfiguration implements WebMvcConfigurer  {

    @Bean("trFilter")
    public ApiTraceFilter requestTracer(ClientSupplier cs, TraceSender sender, @Value("${tracing.application:}") String application) {
        return new ApiTraceFilter(cs, sender, application);
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
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
    	registry.addInterceptor(new ApiTraceInterceptor());
    }
    
    @Bean
    public ClientSupplier clientSupplier() {
        return req-> ofNullable(req.getUserPrincipal())
        		.map(Principal::getName)
        		.orElse(null); //custom request user
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
