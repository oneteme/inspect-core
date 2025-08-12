package org.usf.inspect.rest;

import static org.usf.inspect.core.BeanUtils.logRegistringBean;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * 
 * @author u$f
 *
 */
@Configuration
@ConditionalOnClass(name="org.springframework.web.reactive.function.client.ExchangeFilterFunction")
@ConditionalOnProperty(prefix = "inspect.collector", name = "enabled", havingValue = "true")
public class ReactorModuleConfiguration {

    @Bean
    @DependsOn("inspectContext") //ensure inspectContext is loaded first
    public WebClientFilter webClientFilter() { 
    	logRegistringBean("webClientFilter", WebClientFilter.class);
        return new WebClientFilter();
    }
}
