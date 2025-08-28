package org.usf.inspect.rest;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.BeanUtils.logRegistringBean;
import static org.usf.inspect.core.SessionManager.requireCurrentSession;
import static reactor.core.publisher.Hooks.onEachOperator;
import static reactor.core.publisher.Operators.liftPublisher;

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
	
	ReactorModuleConfiguration() {
		onEachOperator("inspect-hook", p-> {
			var ses = requireCurrentSession();
			return nonNull(ses) ? liftPublisher((scn,sub)-> new CoreSubscriberProxy<>(sub, ses)).apply(p) : p;
		});
	}

	@Bean
	@DependsOn("inspectContext") //ensure inspectContext is loaded first
	public WebClientFilter webClientFilter() { 
		logRegistringBean("webClientFilter", WebClientFilter.class);
		return new WebClientFilter();
	}
}
