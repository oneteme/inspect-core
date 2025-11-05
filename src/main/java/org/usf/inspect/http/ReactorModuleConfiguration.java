package org.usf.inspect.http;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.BeanUtils.logRegistringBean;
import static org.usf.inspect.core.SessionManager.requireCurrentSession;
import static reactor.core.publisher.Hooks.onEachOperator;
import static reactor.core.publisher.Operators.lift;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
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
	
	ReactorModuleConfiguration() {
		logRegistringBean("reactorHook", CoreSubscriberProxy.class);
		onEachOperator("inspect-reactor", lift((scn,sub)-> {
				var ses = requireCurrentSession();
				return nonNull(ses) ? new CoreSubscriberProxy<>(sub, ses) : sub;
			}));
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
