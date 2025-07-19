package org.usf.inspect.rest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@Configuration
@ConditionalOnClass(name="org.springframework.web.reactive.function.client.ExchangeFilterFunction")
@ConditionalOnProperty(prefix = "inspect.collector", name = "enabled", havingValue = "true")
public class ReactorModuleConfiguration {

    @Bean
    @DependsOn("inspectContext") //ensure inspectContext is loaded first
    public WebClientFilter webClientFilter() { 
    	log.debug("loading 'flywayConfigurationCustomizer' bean ..");
        return new WebClientFilter();
    }
}
