package org.usf.inspect.core;

import static java.lang.String.format;
import static java.time.Instant.ofEpochMilli;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.usf.inspect.core.BeanUtils.logLoadingBean;
import static org.usf.inspect.core.BeanUtils.logRegistringBean;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.InspectContext.initializeInspectContext;
import static org.usf.inspect.http.HttpRoutePredicate.compile;
import static org.usf.inspect.jdbc.DataSourceWrapper.wrap;

import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.usf.inspect.http.HandlerExceptionResolverMonitor;
import org.usf.inspect.http.HttpRoutePredicate;
import org.usf.inspect.http.HttpSessionFilter;
import org.usf.inspect.http.RestRequestInterceptor;

import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "inspect.collector", name = "enabled", havingValue = "true")
class InspectConfiguration implements WebMvcConfigurer, ApplicationListener<SpringApplicationEvent>{
	
	private final ApplicationContext appContext;
	
	@Primary
	@Bean("inspectContext")
	InspectContext inspectContext(InspectCollectorConfiguration conf, ApplicationPropertiesProvider provider) {
		logLoadingBean("inspectContext", InspectContext.class);
		var start = ofEpochMilli(appContext.getStartupDate());
		initializeInspectContext(start, conf.validate(), provider); 
		context().traceStartupSession(start); //start session after context is initialized
		return context();
	}
	
    @Bean
    @DependsOn("inspectContext")
	HttpRoutePredicate routePredicate(InspectCollectorConfiguration conf) {
    	logRegistringBean("routePredicate", HttpRoutePredicate.class);
    	return compile(conf.getMonitoring().getHttpRoute());
	}
	
    @Bean //important! name == httpSessionFilter
    @DependsOn("inspectContext") //ensure inspectContext is loaded first
    FilterRegistrationBean<Filter> httpSessionFilter(HttpUserProvider userProvider, HttpRoutePredicate routePredicate) {
    	logRegistringBean("httpSessionFilter", HttpSessionFilter.class);
    	var filter = new HttpSessionFilter(routePredicate, userProvider);
    	var rb = new FilterRegistrationBean<Filter>(filter);
    	rb.setOrder(HIGHEST_PRECEDENCE);
    	rb.addUrlPatterns("/*"); //check that
    	return rb;
    }

	@Override
    public void addInterceptors(InterceptorRegistry registry) {
		if(appContext.containsBean("httpSessionFilter")) {
	    	logRegistringBean("handlerInterceptor", HttpSessionFilter.class);
			var filter = (HttpSessionFilter) appContext.getBean("httpSessionFilter", FilterRegistrationBean.class).getFilter(); //see 
			registry.addInterceptor(filter).order(HIGHEST_PRECEDENCE); //before other interceptors
//				.excludePathPatterns(config.getTrack().getRestSession().excludedPaths())
		}
		else {
			throw new IllegalStateException("cannot find 'httpSessionFilter' bean, check your configuration");
		}
    }
    
    @Bean
    @DependsOn("inspectContext") //ensure inspectContext is loaded first
    RestTemplateCustomizer restTemplateCustomizer() {
    	return rt-> {
			logRegistringBean("restRequestInterceptor", RestRequestInterceptor.class);
			rt.getInterceptors().add(0, new RestRequestInterceptor());
		};
    }

    @Bean
    HandlerExceptionResolverMonitor exceptionResolverMonitor(HttpRoutePredicate routePredicate) {
    	logRegistringBean("exceptionResolverMonitor", HandlerExceptionResolverMonitor.class);
    	return new HandlerExceptionResolverMonitor(routePredicate);
    }
    
    @Bean // Cacheable, Traceable
    @DependsOn("inspectContext") //ensure inspectContext is loaded first
    MethodExecutionMonitor methodExecutionMonitor(AspectUserProvider aspectUser) {
    	logRegistringBean("methodExecutionMonitor", MethodExecutionMonitor.class);
    	return new MethodExecutionMonitor(aspectUser);
    }
    
    @Bean
    @DependsOn("inspectContext") //ensure inspectContext is loaded first
    BeanPostProcessor dataSourceWrapper() {
    	return new BeanPostProcessor() {
    		
    		@Override
    		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
	            return bean instanceof DataSource ds ? wrap(ds, beanName) :  bean;
    		}
		};
    }
    
	@Override
	public void onApplicationEvent(SpringApplicationEvent e) {
		if(e instanceof ApplicationReadyEvent || e instanceof ApplicationFailedEvent) {
			context().traceStartupSession(ofEpochMilli(e.getTimestamp()), 
					e.getSpringApplication().getMainApplicationClass().getName(), "main", 
					e instanceof ApplicationFailedEvent f ? f.getException() : null);
		}
	}
    
    @Bean
    @ConditionalOnMissingBean
    HttpUserProvider httpUserProvider() {
    	logLoadingBean("httpUserProvider", HttpUserProvider.class);
    	return new HttpUserProvider() {};
    }

    @Bean
    @ConditionalOnMissingBean
    AspectUserProvider aspectUserProvider() {
    	logLoadingBean("aspectUserProvider", AspectUserProvider.class);
    	return new AspectUserProvider() {};
    }

    @Bean
    @ConditionalOnMissingBean
    ApplicationPropertiesProvider applicationPropertiesProvider(Environment env) {
    	logLoadingBean("applicationPropertiesProvider", ApplicationPropertiesProvider.class);
    	return new DefaultApplicationPropertiesProvider(env);
    }
    
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "inspect.collector")
    InspectCollectorConfiguration inspectConfigurationProperties(Optional<RemoteServerProperties> dispatching) {
    	logLoadingBean("inspectConfigurationProperties", InspectCollectorConfiguration.class);
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
    @Primary
    @ConfigurationProperties(prefix = "inspect.collector.tracing.remote")
    @ConditionalOnProperty(prefix = "inspect.collector.tracing.remote", name = "mode")
    RemoteServerProperties remoteServerProperties(@Value("${inspect.collector.tracing.remote.mode}") DispatchMode mode) {
    	logLoadingBean("remoteServerProperties", RemoteServerProperties.class);
    	return switch (mode) {
		case REST -> new RestRemoteServerProperties();
		default -> throw new UnsupportedOperationException(format("dispatching type '%s' is not supported, ", mode));
		};
    }
}
