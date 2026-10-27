package org.usf.inspect.core;

import static java.lang.System.getProperty;
import static java.net.InetAddress.getLocalHost;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.InspectCoreModule.jsonMapperBuilder;
import static org.usf.inspect.InstanceType.SERVER;
import static org.usf.inspect.MainSessionType.STARTUP;
import static org.usf.inspect.core.BeanUtils.logLoadingBean;
import static org.usf.inspect.core.BeanUtils.logRegistringBean;
import static org.usf.inspect.core.Constants.ANNOTATION_ENABLED;
import static org.usf.inspect.core.Constants.JDBC_ENABLED;
import static org.usf.inspect.core.Constants.REPORTER;
import static org.usf.inspect.core.DualEventTracer.assertActiveTracer;
import static org.usf.inspect.core.ExecutionTracer.mainSessionTracer;
import static org.usf.inspect.core.Helper.formatLocation;
import static org.usf.inspect.core.TraceHub.hub;
import static org.usf.inspect.sql.DataSourceWrapper.wrap;

import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.usf.inspect.EventTrace;
import org.usf.inspect.InspectCollectorProperties;
import org.usf.inspect.InstanceEnvironment;
import org.usf.inspect.MainSessionUpdate;
import org.usf.inspect.RestRemoteServerProperties;
import org.usf.inspect.TracingProperties;
import org.usf.inspect.aspect.InspectAnnotationConfiguration;

import jakarta.servlet.ServletContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@Configuration
@ConditionalOnBean(InspectConfiguration.class)
public class InspectCoreConfiguration {

	private final ExecutionTracer<?> tracer;
	private final Instant startup;
	
	InspectCoreConfiguration(InspectConfiguration config, ApplicationContext appContext, List<DispatchHook> hooks, ApplicationPropertiesProvider provider) {
		InspectConfiguration.INSTANCE = config;
		var prp = config.getProperties();
		var psh = coreTracePublisher(prp.getTracing(), provider.getNamespace());
		((TraceDispatcherHub)hub()).configure(prp.getScheduling(), psh, hooks, prp.isDebugMode());
		this.startup = ofEpochMilli(appContext.getStartupDate());
		//create startup session immediately after configuring hub, before any other bean is created
		this.tracer = mainSessionTracer(startup, STARTUP, sgn-> sgn.setName("main"));
	}
	
    @Bean
    @ConditionalOnProperty(name = ANNOTATION_ENABLED, havingValue = "true", matchIfMissing = true)
    InspectAnnotationConfiguration methodExecutionMonitor(AspectUserProvider aspectUser, InspectConfiguration config) {
    	logRegistringBean("inspectAnnotationConfiguration", InspectAnnotationConfiguration.class);
    	return new InspectAnnotationConfiguration(aspectUser, config.getProperties().getMonitoring().getAnnotation());
    }

    @Bean
    @ConditionalOnProperty(name = JDBC_ENABLED, havingValue = "true", matchIfMissing = true)
    BeanPostProcessor dataSourceWrapper() {
    	return new BeanPostProcessor() {
    		@Override
    		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
	            return bean instanceof DataSource ds ? wrap(ds, beanName) : bean;
    		}
		};
    }
    
    @Bean
    BeanPostProcessor inspectTaskDecorator() {
    	return new BeanPostProcessor() {
    		@Override
    		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    			if(bean instanceof ThreadPoolTaskExecutor exc) { //context injection for : @Async, Callable, DeferredResult, CompletableFuture 
    				exc.setTaskDecorator(ContextPropagators::wrapRunnable);
    			}
    			//see also SimpleAsyncTaskExecutor & AsyncSupportConfigurer(CallableProcessingInterceptor, DeferredResultProcessingInterceptor)
    			return bean;
    		}
		};
    }
    
    @Bean
    ApplicationListener<SpringApplicationEvent> appEventListener(InspectConfiguration config, ApplicationPropertiesProvider provider, ServletContext servletContext) {
    	var upd = tracer.getUpdate();
    	var instance = newInstanceEnvironment(upd.getId(), startup, config.getProperties(), provider, servletContext);
		try {
			hub().dispatch(instance);
		} catch (TraceDispatchException e) {
			throw new IllegalStateException("cannot dispatch instance environment", e);
		}
		return e-> {
			if(e instanceof ApplicationReadyEvent || e instanceof ApplicationFailedEvent) {
				if(assertActiveTracer(tracer, "InspectConfiguration.appEventListener")) {
					var exp = e instanceof ApplicationFailedEvent f ? f.getException() : null;
					tracer.map((t,o)-> {
						var lct = formatLocation(e.getSpringApplication().getMainApplicationClass().getName(), "main");
						((MainSessionUpdate)t).setLocation(lct);
					}).safeHandle(null, ofEpochMilli(e.getTimestamp()), null, exp);
				}
			}
		};
    }
    
    @Bean
    @ConditionalOnMissingBean //default implementation, can be provided by application
    HttpUserProvider httpUserProvider() {
    	logLoadingBean("httpUserProvider", HttpUserProvider.class);
    	return new HttpUserProvider() {};
    }

    @Bean
    @ConditionalOnMissingBean //default implementation, can be provided by application
    AspectUserProvider aspectUserProvider() {
    	logLoadingBean("aspectUserProvider", AspectUserProvider.class);
    	return new AspectUserProvider() {};
    }

    @Bean
    @ConditionalOnMissingBean //default implementation, can be provided by application
    ApplicationPropertiesProvider applicationPropertiesProvider(Environment env) {
    	logLoadingBean("applicationPropertiesProvider", ApplicationPropertiesProvider.class);
    	return new DefaultApplicationPropertiesProvider(env);
    }
    
	static TracePublisher coreTracePublisher(TracingProperties properties, String namespace) {
		var rsp = properties.getRemote();
		if(rsp instanceof RestRemoteServerProperties prop) {
			var debug = InspectConfiguration.getConfiguration().getProperties().isDebugMode();
			return new RestClientTracePublisher(prop, jsonMapperBuilder(), namespace, debug);
		}
		if(isNull(rsp)) {
			log.warn("remote tracing is disabled, traces will be lost");
			return noExporter(); //no remote agent
		}
		throw new UnsupportedOperationException("unsupported remote " + rsp);
	}
	
	static TracePublisher noExporter() {
		
		return new TracePublisher() {
			
			@Override
			public void register(InstanceEnvironment env) {
				//do nothing
			}
			
			@Override
			public void flush(boolean complete, ProcessingQueue<EventTrace> queue) {
				queue.clear();
			}
		};
	}

	static InstanceEnvironment newInstanceEnvironment(UUID sid, Instant start, InspectCollectorProperties conf, ApplicationPropertiesProvider provider, ServletContext servletContext) {
		var env = new InstanceEnvironment(sid,
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
		env.setNamespace(provider.getNamespace());
		return env;
	}

	static String hostAddress() {
		try {
			return getLocalHost().getHostAddress(); //hostName ?
		} catch (UnknownHostException e) {
			REPORTER.error("InspectCoreConfiguration.hostAddress:", e);
			return null;
		}
	}

	static String collectorID() {
		return "spring-collector/" //use getImplementationTitle
				+ requireNonNullElse(InspectConfiguration.class.getPackage().getImplementationVersion(), "?");
	}
}
