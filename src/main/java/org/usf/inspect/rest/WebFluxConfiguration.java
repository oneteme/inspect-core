package org.usf.inspect.rest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 
 * @author u$f
 *
 */
@Configuration
@ConditionalOnClass(name="org.springframework.web.reactive.function.client.ExchangeFilterFunction")
@ConditionalOnProperty(prefix = "inspect", name = "enabled", havingValue = "true")
public class WebFluxConfiguration {

    @Bean
    @ConditionalOnExpression("${inspect.track.rest-request:true}!=false")
    public WebClientFilter webClientFilter() { 
        return new WebClientFilter();
    }
}
