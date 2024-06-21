package org.usf.traceapi.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;
import static org.usf.traceapi.core.DispatchMode.REMOTE;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.InstanceEnvironment.localInstance;
import static org.usf.traceapi.core.SessionPublisher.complete;
import static org.usf.traceapi.core.SessionPublisher.register;
import static org.usf.traceapi.jdbc.DataSourceWrapper.wrap;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.usf.traceapi.rest.ControllerAdviceAspect;
import org.usf.traceapi.rest.RestRequestInterceptor;
import org.usf.traceapi.rest.RestSessionFilter;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.Filter;

/**
 * 
 * @author u$f
 *
 */
@Configuration
@EnableConfigurationProperties(InspectConfigurationProperties.class)
@ConditionalOnProperty(prefix = "inspect", name = "enabled", havingValue = "true")
public class TraceConfiguration implements WebMvcConfigurer {
	
	private final InspectConfigurationProperties config;

	private RestSessionFilter sessionFilter;
	
	public TraceConfiguration(Environment env, InspectConfigurationProperties conf) {
		this.config = conf.validate();
		if(log.isDebugEnabled()) {
			register(new SessionLogger()); //log first
		}
		if(conf.getMode() == REMOTE) {
			var disp = new RemoteTraceSender(conf.getServer(), localInstance(
					env.getProperty("spring.application.name"),
					env.getProperty("spring.application.version"),
					env.getActiveProfiles()));
			register(new ScheduledDispatchHandler<>(conf.getDispatch(), disp));
		}
	}

	@Override
//  @ConditionalOnExpression("${inspect.track.rest-session:true}!=false")
    public void addInterceptors(InterceptorRegistry registry) {
		if(nonNull(config.getTrack().getRestSession())) {
			registry.addInterceptor(sessionFilter()).order(LOWEST_PRECEDENCE);
//			.excludePathPatterns(config.getTrack().getRestSession().excludedPaths())
		}
    }
	
    @Bean
    @ConditionalOnExpression("${inspect.track.rest-session:true}!=false")
    public FilterRegistrationBean<Filter> apiSessionFilter() {
    	var rb = new FilterRegistrationBean<Filter>(sessionFilter());
    	rb.setOrder(HIGHEST_PRECEDENCE);
    	rb.addUrlPatterns("/*"); //check that
    	return rb;
    }
    
    @Bean
    @ConditionalOnBean(ResponseEntityExceptionHandler.class)
    @ConditionalOnExpression("${inspect.track.rest-session:true}!=false")
    public ControllerAdviceAspect controllerAdviceAspect() {
    	return new ControllerAdviceAspect();
    }
    
    @Bean //do not rename this method see @Qualifier
    @ConditionalOnExpression("${inspect.track.rest-request:true}!=false")
    public RestRequestInterceptor restRequestInterceptor() {
        return new RestRequestInterceptor();
    }
    
    @Bean
    @ConditionalOnExpression("${inspect.track.jdbc-request:true}!=false")
    public BeanPostProcessor dataSourceWrapper() {
    	return new BeanPostProcessor() {
    		@Override
    		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
	            return bean instanceof DataSource ds ? wrap(ds) : bean;
    		}
		};
    }
    
    @Bean
    @ConditionalOnExpression("${inspect.track.main-session:true}!=false")
    public MainSessionAspect traceableAspect() {
    	return new MainSessionAspect();
    }
    
    @PreDestroy
    void shutdown() {
    	complete();
    }
    
    private RestSessionFilter sessionFilter() {
    	if(isNull(sessionFilter)) {
    		sessionFilter = new RestSessionFilter(config.getTrack().getRestSession());
    	}
    	return sessionFilter;
    }
}
