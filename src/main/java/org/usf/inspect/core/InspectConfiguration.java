package org.usf.inspect.core;

import static java.lang.String.format;
import static java.time.Instant.ofEpochMilli;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json;
import static org.usf.inspect.core.BeanUtils.logLoadingBean;
import static org.usf.inspect.core.BeanUtils.logRegistringBean;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.InspectContext.initializeInspectContext;
import static org.usf.inspect.http.HttpRoutePredicate.compile;
import static org.usf.inspect.jdbc.DataSourceWrapper.wrap;

import java.time.Instant;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.usf.inspect.http.HandlerExceptionResolverMonitor;
import org.usf.inspect.http.HttpRoutePredicate;
import org.usf.inspect.http.HttpSessionFilter;
import org.usf.inspect.http.RestRequestInterceptor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
@RequiredArgsConstructor(access = lombok.AccessLevel.PACKAGE)
@ConditionalOnProperty(prefix = "inspect.collector", name = "enabled", havingValue = "true")
public class InspectConfiguration implements WebMvcConfigurer {
	
	private final ApplicationContext appContext;
	
	@Primary
	@Bean("inspectContext")
	Context inspectContext(InspectCollectorConfiguration conf, ApplicationPropertiesProvider provider) {
		logLoadingBean("inspectContext", InspectContext.class);
		initializeInspectContext(conf.validate(), createObjectMapper()); 
		appEventListener(ofEpochMilli(appContext.getStartupDate()), provider); //early bean load
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
    			if(bean instanceof ThreadPoolTaskExecutor ds) {
    				ds.setTaskDecorator(SessionContextManager::aroundRunnable);
    			}
	            return bean instanceof DataSource ds ? wrap(ds, beanName) :  bean;
    		}
		};
    }

    @Bean
    ApplicationListener<SpringApplicationEvent> appEventListener(Instant start, ApplicationPropertiesProvider provider){
    	var mnt = new StartupMonitor();
    	mnt.beforeStartup(start, provider);
		return e-> {
			if(e instanceof ApplicationReadyEvent || e instanceof ApplicationFailedEvent) {
				mnt.afterStartup(
						ofEpochMilli(e.getTimestamp()), 
						e.getSpringApplication().getMainApplicationClass(), "main", 
						e instanceof ApplicationFailedEvent f ? f.getException() : null);
			}
		};
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
    
	static ObjectMapper createObjectMapper() {
		return json()
				.modules(new JavaTimeModule(), coreModule())
				.build()
				.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
		//		.disable(WRITE_DATES_AS_TIMESTAMPS) important! write Instant as double
		//		.configure(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL, true) // force deserialize NamedType if @type is missing
	}
	
	public static SimpleModule coreModule() {
		return new SimpleModule("inspect-core-module").registerSubtypes(
				new NamedType(LogEntry.class, 					"00"),  
				new NamedType(MachineResourceUsage.class, 		"01"),
				new NamedType(RestRemoteServerProperties.class, "02"),
				new NamedType(SessionMaskUpdate.class,			"03"),  
				new NamedType(MainSession2.class,  				"10"), 
				new NamedType(MainSessionCallback.class,  		"11"), 
				new NamedType(HttpSession2.class,  				"20"), 
				new NamedType(HttpSessionCallback.class,  		"21"), 
				new NamedType(LocalRequest2.class, 				"110"),
				new NamedType(LocalRequestCallback.class, 		"111"),
				new NamedType(HttpRequest2.class,  				"120"), 
				new NamedType(HttpRequestCallback.class,  		"121"), 
				new NamedType(DatabaseRequest2.class,			"130"),
				new NamedType(DatabaseRequestCallback.class,	"131"),
				new NamedType(FtpRequest2.class,		  		"140"), 
				new NamedType(FtpRequestCallback.class,  		"141"),
				new NamedType(MailRequest2.class,  				"150"), 
				new NamedType(MailRequestCallback.class,  		"151"), 
				new NamedType(DirectoryRequest2.class,			"160"),
				new NamedType(DirectoryRequestCallback.class,	"161"), 
				new NamedType(HttpSessionStage.class,  			"210"), 
				new NamedType(HttpRequestStage.class,  			"220"), 
				new NamedType(DatabaseRequestStage.class,		"230"), 
				new NamedType(FtpRequestStage.class,  			"240"),
				new NamedType(MailRequestStage.class,  			"250"), 
				new NamedType(DirectoryRequestStage.class,		"260"));
	}
}
