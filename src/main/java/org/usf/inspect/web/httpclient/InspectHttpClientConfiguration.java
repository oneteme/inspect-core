package org.usf.inspect.web.httpclient;

import static org.usf.inspect.core.BeanUtils.logRegistringBean;
import static org.usf.inspect.core.Constants.HTTP_CLIENT_ENABLED;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.usf.inspect.core.InspectCoreConfiguration;

/**
 * 
 * @author u$f
 *
 */
@Configuration
@ConditionalOnBean(InspectCoreConfiguration.class) //inspect enabled
@ConditionalOnClass(name = "org.springframework.boot.web.client.RestTemplateCustomizer") //spring-boot dependency present
@ConditionalOnProperty(name = HTTP_CLIENT_ENABLED, havingValue = "true", matchIfMissing = true)
public class InspectHttpClientConfiguration {

    @Bean
    RestTemplateCustomizer restTemplateCustomizer() {
    	return rt-> {
			logRegistringBean("restRequestInterceptor", HttpRequestInterceptor.class);
			rt.getInterceptors().add(0, new HttpRequestInterceptor());
			//add ClientHttpRequestFactory if needed
		};
    }
}
