package org.usf.inspect.core;

import static java.lang.String.format;
import static java.time.Instant.now;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.nonNull;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.InstanceEnvironment.localInstance;
import static org.usf.inspect.core.SessionManager.endStatupSession;
import static org.usf.inspect.core.SessionManager.startupSession;
import static org.usf.inspect.core.TraceBroadcast.emit;
import static org.usf.inspect.core.TraceBroadcast.register;

import java.time.Instant;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.usf.inspect.jdbc.DataSourceWrapper;
import org.usf.inspect.rest.ControllerAdviceMonitor;
import org.usf.inspect.rest.FilterExecutionMonitor;
import org.usf.inspect.rest.RestRequestInterceptor;

import jakarta.servlet.Filter;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "inspect.collector", name = "enabled", havingValue = "true")
class InspectConfiguration implements WebMvcConfigurer, ApplicationListener<SpringApplicationEvent>{
	
	private final ApplicationContext ctx;
	private final InspectCollectorConfiguration config;
	
	InspectConfiguration(ApplicationContext ctx, InspectCollectorConfiguration conf, ApplicationPropertiesProvider provider) {
		this.ctx = ctx;
		this.config = conf.validate();
		log.info("inspect.properties={}", conf);
		var instance = localInstance(
				ofEpochMilli(ctx.getStartupDate()),
				provider.getName(),
				provider.getVersion(),
				provider.getBranch(),
				provider.getCommitHash(),
				provider.getEnvironment(),
				provider.additionalProperties());
		log.info("inspect enabled on instance={}", instance);
		instance.setConfiguration(conf);
		if(conf.isDebugMode()) {
			register(new SessionTraceDebugger());
		}
		if(conf.getDispatching() instanceof RestDispatchingProperties rest) {
			var disp = new InspectRestClient(rest, instance);
			register(new ScheduledDispatchHandler(conf.getScheduling(), disp));
		}//else unsupported dispatching mode
		initStatupSession(instance.getInstant());
	}
	
    @Bean //important! name == apiSessionFilter
    FilterRegistrationBean<Filter> apiSessionFilter(HttpUserProvider userProvider) {
    	var filter = new FilterExecutionMonitor(config.getTracking().getHttpRoute(), userProvider);
    	var rb = new FilterRegistrationBean<Filter>(filter);
    	rb.setOrder(HIGHEST_PRECEDENCE);
    	rb.addUrlPatterns("/*"); //check that
    	return rb;
    }

	@Override
    public void addInterceptors(InterceptorRegistry registry) {
		if(ctx.containsBean("apiSessionFilter")) {
			var filter = (FilterExecutionMonitor) ctx.getBean("apiSessionFilter", FilterRegistrationBean.class).getFilter(); //see 
			registry.addInterceptor(filter).order(HIGHEST_PRECEDENCE); //before other interceptors
//				.excludePathPatterns(config.getTrack().getRestSession().excludedPaths())
		}
		else {
			log.warn("cannot find 'apiSessionFilter' bean, check your configuration, rest session tracking will not work correctly");
		}
    }
    
    @Bean //important! name == restRequestInterceptor
    RestRequestInterceptor restRequestInterceptor() {
    	log.debug("loading 'RestRequestInterceptor' bean ..");
        return new RestRequestInterceptor();
    }
    
    @Bean
    ControllerAdviceMonitor controllerAdviceAspect() {
    	log.debug("loading 'ControllerAdviceTracker' bean ..");
    	return new ControllerAdviceMonitor();
    }
     
    @Bean
    BeanPostProcessor dataSourceWrapper() {
    	return new BeanPostProcessor() {
    		@Override
    		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
	            return bean instanceof DataSource ds ? new DataSourceWrapper(ds) : bean; //instance of RestTemplate => addInterceptor !!??
    		}
		};
    }
    
    @Bean
    MethodExecutionMonitor methodExecutionMonitor(AspectUserProvider aspectUser) {
    	log.debug("loading 'MethodExecutionMonitorAspect' bean ..");
    	return new MethodExecutionMonitor(aspectUser);
    }
    
    void initStatupSession(Instant start) {
    	var ses = startupSession();
    	try {
	    	ses.setName("main");
	    	ses.setThreadName(threadName());
	    	ses.setStart(start);
    	}
    	finally {
			emit(ses);
		}
    }

	@Override
	public void onApplicationEvent(SpringApplicationEvent e) {
		if(e instanceof ApplicationReadyEvent || e instanceof ApplicationFailedEvent) {
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
		    		ses.setException(mainCauseException(e)); //nullable
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
    HttpUserProvider httpUserProvider() {
    	log.debug("loading 'HttpUserProvider' bean ..");
    	return new HttpUserProvider() {};
    }

    @Bean
    @ConditionalOnMissingBean
    AspectUserProvider aspectUserProvider() {
    	log.debug("loading 'AspectUserProvider' bean ..");
    	return new AspectUserProvider() {};
    }

    @Bean
    @ConditionalOnMissingBean
    static ApplicationPropertiesProvider applicationPropertiesProvider(Environment env) {
    	log.debug("loading 'ApplicationPropertiesProvider' bean ..");
    	return new DefaultApplicationPropertiesProvider(env);
    }
    
    @Bean
    @ConfigurationProperties(prefix = "inspect.collector")
    static InspectCollectorConfiguration inspectConfigurationProperties(Optional<DispatchingProperties> dispatching) {
    	log.debug("loading 'InspectConfigurationProperties' bean ..");
    	var conf = new InspectCollectorConfiguration();
    	if(dispatching.isPresent()) {
    		conf.setDispatching(dispatching.get()); //spring will not call this setter
    	}
		else {
			log.warn("no dispatching type found, dispatching will not be configured");
		}
    	return conf;
    }

    @Bean
    @ConfigurationProperties(prefix = "inspect.collector.dispatching")
    @ConditionalOnProperty(prefix = "inspect.collector.dispatching", name = "mode")
    static DispatchingProperties dispatchingProperties(@Value("${inspect.collector.dispatching.mode}") DispatchTarget mode) {
    	log.debug("loading 'DispatchingProperties' bean ..");
    	return switch (mode) {
		case REST -> new RestDispatchingProperties();
		default -> throw new UnsupportedOperationException(format("dispatching type '%s' is not supported, ", mode));
		};
    }
}
