package org.usf.inspect.http;

import static org.usf.inspect.core.BeanUtils.logRegistringBean;
import static reactor.core.scheduler.Schedulers.onScheduleHook;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.usf.inspect.core.SessionManager;

/**
 * 
 * @author u$f
 *
 */
@Configuration
@ConditionalOnClass(name="org.springframework.web.reactive.function.client.ExchangeFilterFunction")
@ConditionalOnProperty(prefix = "inspect.collector", name = "enabled", havingValue = "true")
public class ReactorModuleConfiguration {
	
	static {
		onScheduleHook("inspect-hook", SessionManager::aroundRunnable);
	}

    @Bean
    @DependsOn("inspectContext") //ensure inspectContext is loaded first
	WebClientCustomizer webClientCustomizer() {
		return wcb->{
			logRegistringBean("webClientFilter", WebClientFilter.class);
			wcb.filter(new WebClientFilter());
		};
	}
}
