package org.usf.traceapi.core;

import static java.lang.String.join;
import static java.lang.System.getProperty;
import static java.net.InetAddress.getLocalHost;
import static java.util.Objects.isNull;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;
import static org.usf.traceapi.core.Helper.application;
import static org.usf.traceapi.core.Helper.basePackage;
import static org.usf.traceapi.core.Helper.log;
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
	
	private ApiSessionFilter sessionFilter;
	
	public TraceConfiguration(Environment env, TraceConfigurationProperties config, @Value("${api.tracing.base-package:}") String pkg) {
		application = applicationInfo(env);
		basePackage = pkg;
		register(config.getUrl().isBlank() 
        		? res-> {} // cache traces !?
        		: new RemoteTraceSender(config));
		log.debug("app.env : {}", application);
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
    
    private ApiSessionFilter sessionFilter() {
    	if(isNull(sessionFilter)) {
    		sessionFilter = new ApiSessionFilter(excludes);
    	}
    	return sessionFilter;
    }

    @Bean //do not rename this method see @Qualifier
    public ApiRequestInterceptor apiRequestInterceptor() {
        return new ApiRequestInterceptor();
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
	            return bean instanceof DataSource ? new DataSourceWrapper((DataSource) bean) : bean;
    		}
		};
    }

    private static ApplicationInfo applicationInfo(Environment env) {
    	return new ApplicationInfo(
				env.getProperty("spring.application.name"),
				env.getProperty("spring.application.version"),
				hostAddress(),
				join(",", env.getActiveProfiles()),
				getProperty("os.name"),
				"java " + getProperty("java.version"));
    }

	private static String hostAddress() {
		try {
			return getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			log.warn("error while getting host address", e);
			return null;
		}
	}
    
}
