package org.usf.inspect.core;

import static java.lang.String.format;
import static java.time.Instant.now;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.nonNull;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.InspectContext.startInspectContext;
import static org.usf.inspect.core.MainSessionType.STARTUP;
import static org.usf.inspect.core.SessionManager.createStartupSession;
import static org.usf.inspect.core.SessionManager.emitStartupSesionEnd;
import static org.usf.inspect.core.SessionManager.emitStartupSession;

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
	private final MainSession session;
	
	InspectConfiguration(ApplicationContext ctx, InspectCollectorConfiguration conf, ApplicationPropertiesProvider provider) {
		this.ctx = ctx;
		var start = ofEpochMilli(ctx.getStartupDate());
		startInspectContext(start, conf.validate(), provider);
		this.session = traceStartupSession(start);
	}
	
    @Bean //important! name == apiSessionFilter
    FilterRegistrationBean<Filter> apiSessionFilter(HttpUserProvider userProvider) {
    	var conf = context().getConfiguration().getMonitoring().getHttpRoute();
    	var filter = new FilterExecutionMonitor(conf, userProvider);
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
    BeanPostProcessor dataSourceWrapper() {
    	return new BeanPostProcessor() {
    		@Override
    		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
	            return bean instanceof DataSource ds ? new DataSourceWrapper(ds) : bean; //instance of RestTemplate => addInterceptor !!??
    		}
		};
    }
    
    @Bean // Cacheable, TraceableStage, ControllerAdvice
    MethodExecutionMonitor methodExecutionMonitor(AspectUserProvider aspectUser) {
    	log.debug("loading 'MethodExecutionMonitorAspect' bean ..");
    	return new MethodExecutionMonitor(aspectUser);
    }

	@Override
	public void onApplicationEvent(SpringApplicationEvent e) {
		if(e instanceof ApplicationReadyEvent || e instanceof ApplicationFailedEvent) {
			traceStartupSession(e.getSource(), e instanceof ApplicationFailedEvent f ? f.getException() : null);
		}
	}
    
    MainSession traceStartupSession(Instant start) {
		var ses = createStartupSession();
		ses.setType(STARTUP.name());
    	ses.setName("main");
    	ses.setStart(start);
    	ses.setThreadName(threadName());
    	emitStartupSession(ses);
    	return ses;
    }
	
	void traceStartupSession(Object appName, Throwable t) {
    	var end = now();
    	var app = mainApplicationClass(appName);
    	session.runSynchronized(()-> {
			session.setLocation(app);
			if(nonNull(t)) {  //nullable
				session.setException(ExceptionInfo.fromException(t));
			}
			session.setEnd(end);
		});
    	emitStartupSesionEnd(session);
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
    static InspectCollectorConfiguration inspectConfigurationProperties(Optional<RemoteServerProperties> dispatching) {
    	log.debug("loading 'InspectConfigurationProperties' bean ..");
    	var conf = new InspectCollectorConfiguration();
    	if(dispatching.isPresent()) {
    		conf.getTracing().setRemote(dispatching.get()); //spring will never call this setter
    	}
		else {
			log.warn("no dispatching type found, dispatching will not be configured");
		}
    	return conf;
    }

    @Bean
    @ConfigurationProperties(prefix = "inspect.collector.tracing.remote")
    @ConditionalOnProperty(prefix = "inspect.collector.tracing.remote", name = "mode")
    static RemoteServerProperties dispatchingProperties(@Value("${inspect.collector.tracing.remote.mode}") DispatchTarget mode) {
    	log.debug("loading 'DispatchingProperties' bean ..");
    	return switch (mode) {
		case REST -> new RestRemoteServerProperties();
		default -> throw new UnsupportedOperationException(format("dispatching type '%s' is not supported, ", mode));
		};
    }
}
