package org.usf.inspect.core;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;
import static org.usf.inspect.core.DispatchTarget.REMOTE;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.Helper.log;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.InstanceEnvironment.localInstance;
import static org.usf.inspect.core.SessionManager.endStatupSession;
import static org.usf.inspect.core.SessionManager.startupSession;
import static org.usf.inspect.core.SessionPublisher.complete;
import static org.usf.inspect.core.SessionPublisher.emit;
import static org.usf.inspect.core.SessionPublisher.register;

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
import org.usf.inspect.jdbc.DataSourceWrapper;
import org.usf.inspect.rest.ControllerAdviceTracker;
import org.usf.inspect.rest.RestRequestInterceptor;
import org.usf.inspect.rest.RestSessionFilter;

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
	private final InstanceEnvironment instance;
	private volatile boolean ready = true;

	private RestSessionFilter sessionFilter;
	
	InspectConfiguration(Environment env, InspectConfigurationProperties conf) {
		this.instance = localInstance(
				env.getProperty("spring.application.name"),
				env.getProperty("spring.application.version"),
				env.getActiveProfiles());
		this.config = conf.validate();
		initStatupSession();
		if(log.isDebugEnabled()) {
			register(new SessionLogger()); //log first
		}
		if(conf.getTarget() == REMOTE) {
			var disp = new InspectRestClient(conf.getServer(), instance);
			register(new ScheduledDispatchHandler<>(conf.getDispatch(), disp, Session::completed));
		}
		log.info("inspect.properties={}", conf);
		log.info("inspect enabled on instance={}", instance);
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
    	if(ready) { //important! PreDestroy called before ApplicationFailedEvent
    		complete();
    	}
    }
    
    private RestSessionFilter sessionFilter() {
    	if(isNull(sessionFilter)) {
    		sessionFilter = new RestSessionFilter(config.getTrack().getRestSession()); //conf !null
    	}
    	return sessionFilter;
    }
    
    void initStatupSession(){
		if(config.getTrack().isStartupSession()) {
			ready = false;
	    	startupSession();
		}
    }

	@Override
	public void onApplicationEvent(SpringApplicationEvent e) {
		if(config.getTrack().isStartupSession() && (e instanceof ApplicationReadyEvent || e instanceof ApplicationFailedEvent)) {
			ready = true;
			emitStartupSession(e.getSource(), e instanceof ApplicationFailedEvent f ? f.getException() : null);
		}
	}
	
	void emitStartupSession(Object appName, Throwable e){
    	var end = now();
        var ses = endStatupSession();
    	if(nonNull(ses)) {
    		try {
    	    	ses.setName("main");
    	    	ses.setLocation(mainApplicationClass(appName));
    	    	ses.setThreadName(threadName());
    	    	ses.setStart(instance.getInstant());
    			ses.setEnd(end);
				ses.setException(mainCauseException(e)); //nullable
				emit(ses);
    		}
    		finally {
				if(nonNull(e)) {
					complete();
				}
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
