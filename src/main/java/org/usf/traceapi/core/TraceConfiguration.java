package org.usf.traceapi.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;
import static org.usf.traceapi.core.Helper.basePackage;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.InstanceEnvironment.localInstance;
import static org.usf.traceapi.core.TraceMultiCaster.register;
import static org.usf.traceapi.jdbc.DataSourceWrapper.wrap;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.usf.traceapi.core.ScheduledDispatcher.Dispatcher;
import org.usf.traceapi.rest.RestRequestInterceptor;
import org.usf.traceapi.rest.RestSessionFilter;
import org.usf.traceapi.rest.WebClientInterceptor;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.Filter;

/**
 * 
 * @author u$f
 *
 */
@Configuration
@EnableConfigurationProperties(TraceConfigurationProperties.class)
@ConditionalOnProperty(prefix = "api.tracing", name = "enabled", havingValue = "true")
public class TraceConfiguration implements WebMvcConfigurer {
	
	@Value("${api.tracing.exclude:}")
	private String[] excludes;
	
	private RestSessionFilter sessionFilter;
	private ScheduledDispatcher<Session> handler;
	
	public TraceConfiguration(Environment env, TraceConfigurationProperties config, @Value("${api.tracing.base-package:}") String pkg) {
		basePackage = pkg;
		var sd = sessionDispatcher(config, env);
		if(nonNull(sd)) {
			this.handler = new ScheduledDispatcher<>(config, sd);
			register(handler::add);
		}
	}

	@Override
    public void addInterceptors(InterceptorRegistry registry) {
    	registry.addInterceptor(sessionFilter())
    	.order(LOWEST_PRECEDENCE)
    	.excludePathPatterns(excludes);
    }
	
    @Bean
    public FilterRegistrationBean<Filter> apiSessionFilter() {
    	var rb = new FilterRegistrationBean<Filter>(sessionFilter());
    	rb.setOrder(HIGHEST_PRECEDENCE);
    	rb.addUrlPatterns("/*"); //check that
    	return rb;
    }
    
    private RestSessionFilter sessionFilter() {
    	if(isNull(sessionFilter)) {
    		sessionFilter = new RestSessionFilter(excludes);
    	}
    	return sessionFilter;
    }

    @Bean //do not rename this method see @Qualifier
    public RestRequestInterceptor apiRequestInterceptor() {
        return new RestRequestInterceptor();
    }
    
    @Bean
    public TraceableAspect traceableAspect() {
    	return new TraceableAspect();
    }
    
    @Bean
    @ConditionalOnClass(name="org.springframework.web.reactive.function.client.ExchangeFilterFunction")
    public WebClientInterceptor webClientBuilder() { 
        return new WebClientInterceptor();
    }

    @Bean
    public BeanPostProcessor dataSourceWrapper() {
    	return new BeanPostProcessor() {
    		@Override
    		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
	            return bean instanceof DataSource ds ? wrap(ds) : bean;
    		}
		};
    }
    
    @PreDestroy
    void shutdown() throws InterruptedException {
    	if(nonNull(handler)) {
    		handler.shutdown();
    	}
    }
    
    static Dispatcher<Session> sessionDispatcher(TraceConfigurationProperties config, Environment env) {
    	Dispatcher<Session> dt1 = null;
		if(log.isDebugEnabled()) {
			dt1 = new SessionLogger();
		}
    	if(isNull(config.getHost()) || config.getHost().isBlank()) {
			log.warn("TraceAPI remote host not configured, {}", config);
			return dt1;
    	}
		var dt2 = new RemoteTraceSender(config, localInstance(
				env.getProperty("spring.application.name"),
				env.getProperty("spring.application.version"),
				env.getActiveProfiles()));
		return isNull(dt1) ? dt2 : dt1.thenDispatch(dt2); //debug 1st after send
    }
}
