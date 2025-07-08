package org.usf.inspect.rest;

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
@ConditionalOnClass(name="org.springframework.web.reactive.function.client.ExchangeFilterFunction")
@ConditionalOnProperty(prefix = "inspect", name = "enabled", havingValue = "true")
public class ReactorModuleConfiguration {

    @Bean
    public WebClientFilter webClientFilter() { 
        return new WebClientFilter();
    }
}
