package org.usf.traceapi.core;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;

import java.security.Principal;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnProperty(prefix = "api.tracing", name = "enabled", havingValue = "true")
public class TraceConfiguration implements WebMvcConfigurer  {
	
	static final ThreadLocal<IncomingRequest> localTrace = new InheritableThreadLocal<>();
	static final Supplier<String> idProvider = ()-> randomUUID().toString();
	
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
    	registry.addInterceptor(new IncomingRequestInterceptor());
    }
	
    @Bean
    public IncomingRequestFilter incomingRequestFilter(
    		@Value("${api.tracing.application:}") String app,
    		@Value("${api.tracing.server.url:}") String url,
    		@Value("${api.tracing.delay:5}") int delay,
    		@Value("${api.tracing.unit:SECONDS}") String unit) {
    	
    	ClientProvider cp = req-> ofNullable(req.getUserPrincipal())
        		.map(Principal::getName)
        		.orElse(null);
    	TraceSender ts = url.isBlank() 
        		? res-> {} 
        		: new RemoteTraceSender(url, delay, TimeUnit.valueOf(unit));
    	return new IncomingRequestFilter(cp, ts, app);
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
