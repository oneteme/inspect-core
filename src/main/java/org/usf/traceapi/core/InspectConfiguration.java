package org.usf.traceapi.core;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;
import static org.usf.traceapi.core.DispatchMode.REMOTE;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.core.Helper.warnNoActiveSession;
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
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.usf.traceapi.jdbc.DataSourceWrapper;
import org.usf.traceapi.rest.ControllerAdviceTracker;
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
class InspectConfiguration implements WebMvcConfigurer, ApplicationListener<SpringApplicationEvent>{
	
	private final InspectConfigurationProperties config;
	private volatile boolean ready = false;

	private RestSessionFilter sessionFilter;
	
	InspectConfiguration(Environment env, InspectConfigurationProperties conf) {
		var inst = localInstance(
				env.getProperty("spring.application.name"),
				env.getProperty("spring.application.version"),
				env.getActiveProfiles());
		this.config = conf.validate();
		initStatupSession(inst);
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
    @ConditionalOnExpression("${inspect.track.rest-session:true}==false")
    Filter cleanThreadLocal() {
    	return (req, res, chn)-> {
    		if(nonNull(localTrace.get())) { //STARTUP session
    			localTrace.remove();
    		}
    		chn.doFilter(req, res);
    	};
    }
    
    @Bean
    @ConditionalOnBean(ResponseEntityExceptionHandler.class)
    @ConditionalOnExpression("${inspect.track.rest-session:true}!=false")
    ControllerAdviceTracker controllerAdviceAspect() {
    	return new ControllerAdviceTracker();
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
    	if(ready) { //destroy called before ApplicationFailedEvent
    		complete();
    	}
    }
    
    private RestSessionFilter sessionFilter() {
    	if(isNull(sessionFilter)) {
    		sessionFilter = new RestSessionFilter(config.getTrack().getRestSession()); //conf !null
    	}
    	return sessionFilter;
    }
    
    void initStatupSession(InstanceEnvironment env){
		if(nonNull(config.getTrack().isMainSession())) {
	    	var s = synchronizedMainSession();
	    	s.setStart(env.getInstant()); //same InstanceEnvironment start
			localTrace.set(s);
		}
    }

	@Override
	public void onApplicationEvent(SpringApplicationEvent e) {
		if(e instanceof ApplicationReadyEvent || e instanceof ApplicationFailedEvent) {
			emitStartupSession(e.getSource(), e instanceof ApplicationFailedEvent f ? f.getException() : null);
			ready = true;
		}
	}
	
	void emitStartupSession(Object appName, Throwable e){
		if(nonNull(config.getTrack().isMainSession())) {
	    	var end = now();
	        var s = localTrace.get();
	        if(nonNull(s)) {
	        	if(s instanceof MainSession ms) {
	        		try {
	        	    	ms.setName("main");
	        	    	ms.setType(STARTUP.name());
	        	    	ms.setLocation(mainApplicationClass(appName));
	        	    	ms.setThreadName(threadName());
	        			ms.setEnd(end);
	        			if(nonNull(e)) {
	        				ms.setException(mainCauseException(e));
	        			}
        				emit(ms);
        				if(nonNull(e)) {
        					complete();
        				}
	        		}
	        		finally {
	        			localTrace.remove();
					}
	        	}
	        	else {
	        		log.warn("unexpected session type {}", s);
	        	}
	        }
	        else {
	        	warnNoActiveSession("startup");
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
