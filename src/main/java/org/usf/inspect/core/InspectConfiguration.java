package org.usf.inspect.core;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.usf.inspect.core.DispatchTarget.REMOTE;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.Helper.log;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.InstanceEnvironment.localInstance;
import static org.usf.inspect.core.MetricsBroadcast.emit;
import static org.usf.inspect.core.MetricsBroadcast.register;
import static org.usf.inspect.core.SessionManager.endStatupSession;
import static org.usf.inspect.core.SessionManager.startupSession;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
	private final HttpUserProvider httpUser;
	private final AspectUserProvider aspectUser;
	
	private RestSessionFilter sessionFilter;
	
	InspectConfiguration(InspectConfigurationProperties conf, ApplicationPropertiesProvider provider, HttpUserProvider httpUser, AspectUserProvider aspectUser) {
		this.instance = localInstance(
				provider.getName(),
				provider.getVersion(),
				provider.getBranch(),
				provider.getCommitHash(),
				provider.getEnvironment());
		this.config = conf.validate();
		this.httpUser = httpUser;
		this.aspectUser = aspectUser;
		initStatupSession();
		if(log.isDebugEnabled()) {
			register(new SessionLogger()); //log first
		}
		if(conf.getTarget() == REMOTE) {
			var disp = new InspectRestClient(conf.getServer(), instance);
			register(new ScheduledDispatchHandler<>(conf.getDispatch(), disp));
		}
		log.info("inspect.properties={}", conf);
		log.info("inspect enabled on instance={}", instance);
	}

	@Override
//  @ConditionalOnExpression("${inspect.track.rest-session:true}!=false")
    public void addInterceptors(InterceptorRegistry registry) {
		if(nonNull(config.getTrack().getRestSession())) {
			registry.addInterceptor(sessionFilter()).order(HIGHEST_PRECEDENCE); //before auth.,.. interceptors 
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
    	return new MainSessionAspect(aspectUser);
    }
    
    private RestSessionFilter sessionFilter() {
    	if(isNull(sessionFilter)) {
    		sessionFilter = new RestSessionFilter(config.getTrack().getRestSession(), httpUser); //conf !null
    	}
    	return sessionFilter;
    }
    
    void initStatupSession(){
		if(config.getTrack().isStartupSession()) {
	    	var ses = startupSession();
	    	try {
		    	ses.setName("main");
		    	ses.setThreadName(threadName());
		    	ses.setStart(instance.getInstant());
	    	}
	    	finally {
				emit(ses);
			}
		}
    }

	@Override
	public void onApplicationEvent(SpringApplicationEvent e) {
		if(config.getTrack().isStartupSession() && (e instanceof ApplicationReadyEvent || e instanceof ApplicationFailedEvent)) {
			emitStartupSession(e.getSource(), e instanceof ApplicationFailedEvent f ? f.getException() : null);
		}
	}
	
	void emitStartupSession(Object appName, Throwable e){
    	var end = now();
        var ses = endStatupSession();
    	if(nonNull(ses)) {
    		try {
    	    	ses.setLocation(mainApplicationClass(appName));
    	    	if(nonNull(e)) {
		    		ses.appendException(mainCauseException(e)); //nullable
		    	}
				ses.setEnd(end);
    		}
    		finally {
				emit(ses);
			}
    	}
	}
	
    static String mainApplicationClass(Object source) {
    	return (source instanceof SpringApplication app 
    			? app.getMainApplicationClass()
    			: SpringApplication.class)
    			.getCanonicalName();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public static ApplicationPropertiesProvider springProperties(Environment env) {
    	return new DefaultApplicationPropertiesProvider(env);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public static HttpUserProvider httpUserProvider() {
    	return HttpUserProvider::getUserPrincipal;
    }

    @Bean
    @ConditionalOnMissingBean
    public static AspectUserProvider aspectUserProvider() {
    	return AspectUserProvider::getAspectUser;
    }
}
