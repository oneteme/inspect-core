package org.usf.traceapi.core;

import static java.lang.String.join;
import static java.lang.System.getProperty;
import static java.net.InetAddress.getLocalHost;
import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;
import static org.usf.traceapi.core.Helper.basePackage;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.InstantType.SERVER;
import static org.usf.traceapi.core.TraceMultiCaster.register;

import java.net.UnknownHostException;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.usf.traceapi.jdbc.DataSourceWrapper;
import org.usf.traceapi.rest.RestRequestInterceptor;
import org.usf.traceapi.rest.RestSessionFilter;

import jakarta.servlet.Filter;

/**
 * 
 * @author u$f
 *
 */
@Configuration
@EnableConfigurationProperties(TraceConfigurationProperties.class)
@ConditionalOnProperty(prefix = "api.tracing", name = "enabled", havingValue = "true")
public class TraceConfiguration implements WebMvcConfigurer {
	
	@Value("${api.tracing.exclude:}")
	private String[] excludes;
	
	private RestSessionFilter sessionFilter;
	
	public TraceConfiguration(Environment env, TraceConfigurationProperties config, @Value("${api.tracing.base-package:}") String pkg) {
		var inst = currentInstance(env);
		basePackage = pkg;
		register(config.getHost().isBlank() 
        		? res-> {} // cache traces !?
        		: new RemoteTraceSender(config, inst));
		log.info("app.env : {}", inst);
	}

	@Override
    public void addInterceptors(InterceptorRegistry registry) {
    	registry.addInterceptor(sessionFilter())
    	.order(LOWEST_PRECEDENCE)
    	.excludePathPatterns(excludes);
    }
	
    @Bean
    public FilterRegistrationBean<Filter> apiSessionFilter() {
    	var rb = new FilterRegistrationBean<Filter>(sessionFilter());
    	rb.setOrder(HIGHEST_PRECEDENCE);
    	rb.addUrlPatterns("/*"); //check that
    	return rb;
    }
    
    private RestSessionFilter sessionFilter() {
    	if(isNull(sessionFilter)) {
    		sessionFilter = new RestSessionFilter(excludes);
    	}
    	return sessionFilter;
    }

    @Bean //do not rename this method see @Qualifier
    public RestRequestInterceptor apiRequestInterceptor() {
        return new RestRequestInterceptor();
    }
    
    @Bean
    public TraceableAspect traceableAspect() {
    	return new TraceableAspect();
    }

    @Bean
    public BeanPostProcessor dataSourceWrapper() {
    	return new BeanPostProcessor() {
    		@Override
    		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
	            return bean instanceof DataSource ds ? new DataSourceWrapper(ds) : bean;
    		}
		};
    }

    private static InstanceEnvironment currentInstance(Environment env) {
    	return new InstanceEnvironment(
				env.getProperty("spring.application.name"),
				env.getProperty("spring.application.version"),
				hostAddress(),
				join(",", env.getActiveProfiles()),
				getProperty("os.name"), //version ? window 10 / Linux
				"java " + getProperty("java.version"),
				getProperty("user.name"),
				SERVER,
				now(),
				collectorID());
	}

	private static String hostAddress() {
		try {
			return getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			log.warn("error while getting host address", e);
			return null;
		}
	}
	
	private static String collectorID() {
		return "spring-collector-v" //use getImplementationTitle
				+ ofNullable(TraceConfiguration.class.getPackage().getImplementationVersion())
				.orElse("?");
	}
}
