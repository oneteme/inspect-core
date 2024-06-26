package org.usf.traceapi.core;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;
import static org.usf.traceapi.core.DispatchMode.REMOTE;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.InstanceEnvironment.localInstance;
import static org.usf.traceapi.core.MainSession.synchronizedMainSession;
import static org.usf.traceapi.core.MainSessionType.STARTUP;
import static org.usf.traceapi.core.SessionPublisher.complete;
import static org.usf.traceapi.core.SessionPublisher.emit;
import static org.usf.traceapi.core.SessionPublisher.register;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.usf.traceapi.jdbc.DataSourceWrapper;
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
class InspectConfiguration implements WebMvcConfigurer {
	
	private final InspectConfigurationProperties config;

	private RestSessionFilter sessionFilter;
	
	InspectConfiguration(Environment env, InspectConfigurationProperties conf) {
		var inst = localInstance(
				env.getProperty("spring.application.name"),
				env.getProperty("spring.application.version"),
				env.getActiveProfiles());
		initStatupSession(inst);
		this.config = conf.validate();
		if(log.isDebugEnabled()) {
			register(new SessionLogger()); //log first
		}
		if(conf.getMode() == REMOTE) {
			var disp = new InspectRestClient(conf.getServer(), inst);
			register(new ScheduledDispatchHandler<>(conf.getDispatch(), disp));
		}
		log.info("inspect.properties={}", conf);
		log.info("inspect enabled on instance={}", inst);
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
    FilterRegistrationBean<Filter> apiSessionFilter() {
    	var rb = new FilterRegistrationBean<Filter>(sessionFilter());
    	rb.setOrder(HIGHEST_PRECEDENCE);
    	rb.addUrlPatterns("/*"); //check that
    	return rb;
    }
    
    @Bean
    @ConditionalOnBean(ResponseEntityExceptionHandler.class)
    @ConditionalOnExpression("${inspect.track.rest-session:true}!=false")
    ControllerAdviceAspect controllerAdviceAspect() {
    	return new ControllerAdviceAspect();
    }
    
    @Bean //do not rename this method see @Qualifier
    @ConditionalOnExpression("${inspect.track.rest-request:true}!=false")
    RestRequestInterceptor restRequestInterceptor() {
        return new RestRequestInterceptor();
    }
    
    @Bean
    @ConditionalOnExpression("${inspect.track.jdbc-request:true}!=false")
    BeanPostProcessor dataSourceWrapper() {
    	return new BeanPostProcessor() {
    		@Override
    		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
	            return bean instanceof DataSource ds ? new DataSourceWrapper(ds) : bean;
    		}
		};
    }
    
    @Bean
    @ConditionalOnExpression("${inspect.track.main-session:true}!=false")
    MainSessionAspect traceableAspect() {
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
    
    void initStatupSession(InstanceEnvironment env){
    	var s = synchronizedMainSession();
    	s.setStart(env.getInstant()); //same InstanceEnvironment start
		localTrace.set(s);
    }
    
    @EventListener(ApplicationReadyEvent.class)
    void emitStatupSession(ApplicationReadyEvent v) {
    	var end = now();
        var s = localTrace.get();
        if(nonNull(s)) {
        	if(s instanceof MainSession ms) {
        		try {
        	    	ms.setName("main");
        	    	ms.setType(STARTUP.name());
        	    	ms.setLocation(mainApplicationClass(v.getSource()));
        			ms.setEnd(end);
        			emit(ms);
        		}
        		finally {
        			localTrace.remove();
				}
        	}
        	else {
        		log.warn("unexpected session type {}", s);
        	}
        }
    }
    
    static String mainApplicationClass(Object source) {
    	return (source instanceof SpringApplication app 
    			? app.getMainApplicationClass()
    			: SpringApplication.class)
    			.getCanonicalName();
    }
}
