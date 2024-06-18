package org.usf.traceapi.rest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 
 * @author u$f
 *
 */
@Configuration
@ConditionalOnProperty(prefix = "api.tracing", name = "enabled", havingValue = "true")
@ConditionalOnClass(name="org.springframework.web.reactive.function.client.ExchangeFilterFunction")
public class WebFluxConfiguration {

    @Bean
    public WebClientFilter webClientFilter() { 
        return new WebClientFilter();
    }
}
