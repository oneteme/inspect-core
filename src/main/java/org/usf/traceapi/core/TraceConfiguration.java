package org.usf.traceapi.core;

import static java.lang.String.join;
import static java.lang.System.getProperty;
import static java.net.InetAddress.getLocalHost;
import static org.usf.traceapi.core.Helper.application;

import java.net.UnknownHostException;
import java.util.Collection;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(TraceConfigurationProperties.class)
@ConditionalOnProperty(prefix = "api.tracing", name = "enabled", havingValue = "true")
public class TraceConfiguration implements WebMvcConfigurer {
	
	public TraceConfiguration(Environment env) {
		application = new ApplicationInfo(
				env.getProperty("spring.application.name"),
				env.getProperty("spring.application.version"),
				hostAddress(),
				join(",", env.getActiveProfiles()),
				getProperty("os.name"),
				"java " + getProperty("java.version"));
	}
	
	@Override
    public void addInterceptors(InterceptorRegistry registry) {
    	registry.addInterceptor(new IncomingRequestInterceptor());
    }
	
    @Bean
    public IncomingRequestFilter incomingRequestFilter(TraceSender sender, @Value("${api.tracing.exclude}") String[] excludes) {
    	return new IncomingRequestFilter(sender, excludes);
    }
    
    @Bean
    public TraceableAspect traceableAspect(TraceSender sender) {
    	return new TraceableAspect(sender);
    }

    @Bean //do not rename this method see @Qualifier
    public OutcomingRequestInterceptor outcomingRequestInterceptor() {
        return new OutcomingRequestInterceptor();
    }

    @Bean
    public BeanPostProcessor dataSourceWrapper() {
    	return new BeanPostProcessor() {
    		@Override
    		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
	            return bean instanceof DataSource 
	            		? new DataSourceWrapper((DataSource) bean) 
	            		: bean;
    		}
		};
    }

    @Bean
    public TraceSender sender(TraceConfigurationProperties config) {
    	return config.getHost().isBlank() 
        		? res-> {} 
        		: new RemoteTraceSender(config);
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
