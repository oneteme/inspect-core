package org.usf.inspect.web.mvc;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.usf.inspect.core.BeanUtils.logRegistringBean;
import static org.usf.inspect.core.Constants.WEB_MVC_ENABLED;
import static org.usf.inspect.web.mvc.HttpRoutePredicate.compile;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.usf.inspect.core.HttpUserProvider;
import org.usf.inspect.core.InspectConfiguration;
import org.usf.inspect.core.InspectCoreConfiguration;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletRequestListener;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnBean(InspectCoreConfiguration.class) //inspect enabled
@ConditionalOnClass(name = "org.springframework.web.servlet.config.annotation.WebMvcConfigurer")
@ConditionalOnProperty(name = WEB_MVC_ENABLED, havingValue = "true", matchIfMissing = true)
public class InspectWebMvcConfiguration implements WebMvcConfigurer {

	private final ApplicationContext appContext;
	
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
	HttpRoutePredicate routePredicate(InspectConfiguration conf) {
    	logRegistringBean("routePredicate", HttpRoutePredicate.class);
    	return compile(conf.getProperties().getMonitoring().getWebmvc());
	}

    @Bean
    ServletListenerRegistrationBean<ServletRequestListener> inspectRequestListener(HttpRoutePredicate routePredicate) {
        return new ServletListenerRegistrationBean<>(new InspectServletRequestListener(routePredicate));
	}
    
    @Bean //important! name == httpSessionFilter
    FilterRegistrationBean<Filter> httpSessionFilter(HttpUserProvider userProvider, HttpRoutePredicate routePredicate) {
    	logRegistringBean("httpSessionFilter", HttpSessionFilter.class);
    	var filter = new HttpSessionFilter(userProvider);
    	var rb = new FilterRegistrationBean<Filter>(filter);
    	rb.setOrder(HIGHEST_PRECEDENCE);
    	rb.addUrlPatterns("/*"); //check that
    	return rb;
    }
    
    @Bean
    InspectHandlerExceptionResolver exceptionResolverMonitor() {
    	logRegistringBean("exceptionResolverMonitor", InspectHandlerExceptionResolver.class);
    	return new InspectHandlerExceptionResolver();
    }
}
