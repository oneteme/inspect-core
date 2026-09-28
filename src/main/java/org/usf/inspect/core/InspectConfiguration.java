package org.usf.inspect.core;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.net.InetAddress.getLocalHost;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json;
import static org.usf.inspect.core.BeanUtils.logLoadingBean;
import static org.usf.inspect.core.BeanUtils.logRegistringBean;
import static org.usf.inspect.core.ExecutionTracer.forMainSession;
import static org.usf.inspect.core.Helper.formatLocation;
import static org.usf.inspect.core.InstanceType.SERVER;
import static org.usf.inspect.core.SessionContextManager.createStartupSession;
import static org.usf.inspect.core.SessionContextManager.nextId;
import static org.usf.inspect.core.TraceHub.hub;
import static org.usf.inspect.http.HttpRoutePredicate.compile;
import static org.usf.inspect.jdbc.DataSourceWrapper.wrap;

import java.net.UnknownHostException;
import java.time.Instant;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.usf.inspect.http.HttpRequestInterceptor;
import org.usf.inspect.http.HttpRoutePredicate;
import org.usf.inspect.http.HttpSessionFilter;
import org.usf.inspect.http.InspectHandlerExceptionResolver;
import org.usf.inspect.http.InspectServletRequestListener;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequestListener;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "inspect.collector", name = "enabled", havingValue = "true")
public class InspectConfiguration implements WebMvcConfigurer {
	
	private final ApplicationContext appContext;
	private final ExecutionTracer<?> tracer;
	
	InspectConfiguration(ApplicationContext appContext, Environment env) {
        ((TraceDispatcherHub)hub()).configure(loadConfiguration(env)).start();
        this.appContext = appContext;
        this.tracer = forMainSession(()-> { //create startup session immediately after configuring hub, before any other bean is created
			var sgn = createStartupSession(ofEpochMilli(appContext.getStartupDate()));
			sgn.setName("main"); //try location = getProperty("sun.java.command") //Spring boot
			return sgn;
		});
	}
	
    @Bean
	HttpRoutePredicate routePredicate(InspectCollectorConfiguration conf) {
    	logRegistringBean("routePredicate", HttpRoutePredicate.class);
    	return compile(conf.getMonitoring().getHttpRoute());
	}
	
    @Bean //important! name == httpSessionFilter
    FilterRegistrationBean<Filter> httpSessionFilter(HttpUserProvider userProvider, HttpRoutePredicate routePredicate) {
    	logRegistringBean("httpSessionFilter", HttpSessionFilter.class);
    	var filter = new HttpSessionFilter(routePredicate, userProvider);
    	var rb = new FilterRegistrationBean<Filter>(filter);
    	rb.setOrder(HIGHEST_PRECEDENCE);
    	rb.addUrlPatterns("/*"); //check that
    	return rb;
    }

    @Bean
    ServletListenerRegistrationBean<ServletRequestListener> inspectRequestListener() {
        return new ServletListenerRegistrationBean<>(new InspectServletRequestListener());
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
    RestTemplateCustomizer restTemplateCustomizer() {
    	return rt-> {
			logRegistringBean("restRequestInterceptor", HttpRequestInterceptor.class);
			rt.getInterceptors().add(0, new HttpRequestInterceptor());
			//add ClientHttpRequestFactory if needed
		};
    }

    @Bean
    InspectHandlerExceptionResolver exceptionResolverMonitor(HttpRoutePredicate routePredicate) {
    	logRegistringBean("exceptionResolverMonitor", InspectHandlerExceptionResolver.class);
    	return new InspectHandlerExceptionResolver(routePredicate);
    }
    
    @Bean // Cacheable, Traceable
    MethodExecutionMonitor methodExecutionMonitor(AspectUserProvider aspectUser) {
    	logRegistringBean("methodExecutionMonitor", MethodExecutionMonitor.class);
    	return new MethodExecutionMonitor(aspectUser);
    }
    
    @Bean
    BeanPostProcessor dataSourceWrapper() {
    	return new BeanPostProcessor() {
    		
    		@Override
    		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    			if(bean instanceof ThreadPoolTaskExecutor exc) { //context injection for : @Async, Callable, DeferredResult, CompletableFuture 
    				exc.setTaskDecorator(SessionPropagator::wrapRunnable);
    			}
    			//see also SimpleAsyncTaskExecutor & AsyncSupportConfigurer(CallableProcessingInterceptor, DeferredResultProcessingInterceptor)
	            return bean instanceof DataSource ds ? wrap(ds, beanName) : bean;
    		}
		};
    }
    
    @Bean
    ApplicationListener<SpringApplicationEvent> appEventListener(ApplicationPropertiesProvider provider, ServletContext servletContext){
    	var start = ofEpochMilli(appContext.getStartupDate());
    	var instance = newInstanceEnvironment(start, hub().getConfiguration(), provider, servletContext);
		hub().dispatch(instance);
		return e-> {
			if(e instanceof ApplicationReadyEvent || e instanceof ApplicationFailedEvent) {
				if(nonNull(tracer)) {
					var exp = e instanceof ApplicationFailedEvent f ? f.getException() : null;
					tracer.map((t,o)-> {
						var lct = formatLocation(e.getSpringApplication().getMainApplicationClass().getName(), "main");
						((MainSessionUpdate)t).setLocation(lct);
					}).safeHandle(null, ofEpochMilli(e.getTimestamp()), null, exp);
				}
				else {
					hub().reportMessage("InspectConfiguration.appEventListener", "tracer is null, cannot trace event " + e.getClass().getSimpleName());
				}
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
    
    //TODO move this
	public static ObjectMapper createObjectMapper() {
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
				new NamedType(ExceptionTrace.class,				"04"), 
				new NamedType(SessionEvent.class,				"05"),
				new NamedType(MainSessionSignal.class,  		"10"),
				new NamedType(MainSessionUpdate.class,  		"11"), 
				new NamedType(HttpSessionSignal.class,  		"20"), 
				new NamedType(HttpSessionUpdate.class,  		"21"), 
				new NamedType(LocalRequestSignal.class, 		"110"),
				new NamedType(LocalRequestUpdate.class, 		"111"),
				new NamedType(HttpRequestSignal.class,  		"120"), 
				new NamedType(HttpRequestUpdate.class,  		"121"), 
				new NamedType(DatabaseRequestSignal.class,		"130"),
				new NamedType(DatabaseRequestUpdate.class,		"131"),
				new NamedType(FtpRequestSignal.class,		  	"140"), 
				new NamedType(FtpRequestUpdate.class,  			"141"),
				new NamedType(MailRequestSignal.class,  		"150"), 
				new NamedType(MailRequestUpdate.class,  		"151"), 
				new NamedType(DirectoryRequestSignal.class,		"160"),
				new NamedType(DirectoryRequestUpdate.class,		"161"), 
				new NamedType(HttpSessionStage.class,  			"210"), 
				new NamedType(HttpRequestStage.class,  			"220"), 
				new NamedType(DatabaseRequestStage.class,		"230"), 
				new NamedType(FtpRequestStage.class,  			"240"),
				new NamedType(MailRequestStage.class,  			"250"), 
				new NamedType(DirectoryRequestStage.class,		"260"));
	}
	

	static InstanceEnvironment newInstanceEnvironment(Instant start, InspectCollectorConfiguration conf, ApplicationPropertiesProvider provider, ServletContext servletContext) {
		return new InstanceEnvironment(nextId(),
				start, SERVER,
				provider.getName(), 
				provider.getVersion(),
				provider.getEnvironment(),
				hostAddress(),
				getProperty("os.name") + "/" + getProperty("os.version") + " (" + getProperty("os.arch") + ")",
				"java/" + getProperty("java.version"),
				getProperty("user.name"),
				provider.getBranch(),
				provider.getCommitHash(),
				collectorID(),
				provider.additionalProperties(servletContext),
				conf);
	}

	static String hostAddress() {
		try {
			return getLocalHost().getHostAddress(); //hostName ?
		} catch (UnknownHostException e) {
			log.warn("error while getting host address {}", e.getMessage());
			return null;
		}
	}

	static String collectorID() {
		return "spring-collector/" //use getImplementationTitle
				+ requireNonNullElse(InspectConfiguration.class.getPackage().getImplementationVersion(), "?");
	}
	
	static InspectCollectorConfiguration loadConfiguration(Environment env) {
		log.info("loading 'inspect.collector' configuration from environment");
		var bnd = Binder.get(env);
        var conf = bnd.bind("inspect.collector", InspectCollectorConfiguration.class)
        	.orElseThrow(()-> new IllegalStateException("cannot bind 'inspect.collector' configuration"));
        var mode = bnd.bind("inspect.collector.tracing.remote.mode", DispatchMode.class).orElse(null);
        if(nonNull(mode)) {
        	var expr = switch (mode) {
        	case REST -> bnd.bind("inspect.collector.tracing.remote", RestRemoteServerProperties.class)
        		.orElseThrow(()-> new IllegalStateException("cannot bind 'inspect.collector' configuration"));
        	default -> throw new UnsupportedOperationException(format("dispatching type '%s' is not supported, ", mode));
        	};
        	conf.getTracing().setRemote(expr);
        }
        conf.validate();
        return conf;
	}
}
