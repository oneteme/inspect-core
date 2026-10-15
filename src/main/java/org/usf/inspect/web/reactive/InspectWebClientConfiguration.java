package org.usf.inspect.web.reactive;

import static org.usf.inspect.core.BeanUtils.logRegistringBean;
import static org.usf.inspect.core.Constants.WEB_CLIENT_ENABLED;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
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
@ConditionalOnClass(name = "org.springframework.web.reactive.function.client.ExchangeFilterFunction")
@ConditionalOnProperty(name = WEB_CLIENT_ENABLED, havingValue = "true", matchIfMissing = true)
public class InspectWebClientConfiguration {

	@Bean
	@ConditionalOnClass(name = "org.springframework.web.reactive.function.client.ExchangeFilterFunction")
	WebClientCustomizer webClientCustomizer() {
		return wcb-> {
			logRegistringBean("webClientFilter", WebClientFilter.class);
			wcb.filter(new WebClientFilter());
		};
	}
}
