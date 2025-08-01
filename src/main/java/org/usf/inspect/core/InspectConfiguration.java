package org.usf.inspect.core;

import static java.lang.String.format;
import static java.time.Instant.ofEpochMilli;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.InspectContext.initializeInspectContext;

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
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.usf.inspect.jdbc.DataSourceWrapper;
import org.usf.inspect.rest.FilterExecutionMonitor;
import org.usf.inspect.rest.RestRequestInterceptor;

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
		var start = ofEpochMilli(appContext.getStartupDate());
		initializeInspectContext(start, conf.validate(), provider); 
		context().traceStartupSession(start); //start session after context is initialized
		return context();
	}
	
    @Bean //important! name == apiSessionFilter
    @DependsOn("inspectContext") //ensure inspectContext is loaded first
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
		if(appContext.containsBean("apiSessionFilter")) {
			var filter = (FilterExecutionMonitor) appContext.getBean("apiSessionFilter", FilterRegistrationBean.class).getFilter(); //see 
			registry.addInterceptor(filter).order(HIGHEST_PRECEDENCE); //before other interceptors
//				.excludePathPatterns(config.getTrack().getRestSession().excludedPaths())
		}
		else {
			context().reportError("cannot find 'apiSessionFilter' bean, check your configuration, rest session tracking will not work correctly");
		}
    }
    
    @Bean //important! name == restRequestInterceptor
    @DependsOn("inspectContext") //ensure inspectContext is loaded first
    RestRequestInterceptor restRequestInterceptor() {
    	log.debug("loading 'RestRequestInterceptor' bean ..");
        return new RestRequestInterceptor();
    }
    
    //TODO org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
    @Bean
    @DependsOn("inspectContext") //ensure inspectContext is loaded first
    BeanPostProcessor dataSourceWrapper(RestRequestInterceptor interceptor) {
    	return new BeanPostProcessor() {
    		
    		private boolean intecept;
    		
    		@Override
    		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    			if(bean instanceof DataSource ds && bean.getClass() != DataSourceWrapper.class) {
    		    	log.info("wrapping spring DataSource '{}' ..", bean.getClass());
    				bean = new DataSourceWrapper(ds);
    			}
    			else if(!intecept && bean instanceof RestTemplate rt) {
    		    	log.info("adding 'RestRequestInterceptor' on {}", bean.getClass());
    				var arr = rt.getInterceptors();
    				arr.add(0, interceptor);
    				rt.setInterceptors(arr);
    				intecept = true; //only one time
    			}
    			else if(!intecept && bean instanceof RestTemplateBuilder rtb) {
    		    	log.info("adding 'RestRequestInterceptor' on {}", bean.getClass());
    				rtb.additionalInterceptors(interceptor); //order !
    				intecept = true; //only one time
    			}
	            return bean; //instance of RestTemplate => addInterceptor !!??
    		}
		};
    }
    
    @Bean // Cacheable, TraceableStage, ControllerAdvice
    @DependsOn("inspectContext") //ensure inspectContext is loaded first
    MethodExecutionMonitor methodExecutionMonitor(AspectUserProvider aspectUser) {
    	log.debug("loading 'MethodExecutionMonitorAspect' bean ..");
    	return new MethodExecutionMonitor(aspectUser);
    }

	@Override
	public void onApplicationEvent(SpringApplicationEvent e) {
		if(e instanceof ApplicationReadyEvent || e instanceof ApplicationFailedEvent) {
			context().traceStartupSession(ofEpochMilli(e.getTimestamp()), 
					e.getSpringApplication().getMainApplicationClass().getName(), 
					e instanceof ApplicationFailedEvent f ? f.getException() : null);
		}
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
    ApplicationPropertiesProvider applicationPropertiesProvider(Environment env) {
    	log.debug("loading 'ApplicationPropertiesProvider' bean ..");
    	return new DefaultApplicationPropertiesProvider(env);
    }
    
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "inspect.collector")
    InspectCollectorConfiguration inspectConfigurationProperties(Optional<RemoteServerProperties> dispatching) {
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
    @Primary
    @ConfigurationProperties(prefix = "inspect.collector.tracing.remote")
    @ConditionalOnProperty(prefix = "inspect.collector.tracing.remote", name = "mode")
    RemoteServerProperties dispatchingProperties(@Value("${inspect.collector.tracing.remote.mode}") DispatchMode mode) {
    	log.debug("loading 'DispatchingProperties' bean ..");
    	return switch (mode) {
		case REST -> new RestRemoteServerProperties();
		default -> throw new UnsupportedOperationException(format("dispatching type '%s' is not supported, ", mode));
		};
    }
}
