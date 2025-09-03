package org.usf.inspect.http;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.BeanUtils.logRegistringBean;
import static org.usf.inspect.core.SessionManager.requireCurrentSession;
import static reactor.core.publisher.Hooks.onEachOperator;
import static reactor.core.publisher.Operators.liftPublisher;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient.Builder;

/**
 * 
 * @author u$f
 *
 */
@Configuration
@ConditionalOnClass(name="org.springframework.web.reactive.function.client.ExchangeFilterFunction")
@ConditionalOnProperty(prefix = "inspect.collector", name = "enabled", havingValue = "true")
public class ReactorModuleConfiguration implements WebClientCustomizer {
	
	ReactorModuleConfiguration() {
		onEachOperator("inspect-hook", p-> {
			var ses = requireCurrentSession();
			return nonNull(ses) ? liftPublisher((scn,sub)-> new CoreSubscriberProxy<>(sub, ses)).apply(p) : p;
		});
	}

	@Override
	public void customize(Builder webClientBuilder) {
		logRegistringBean("webClientFilter", WebClientFilter.class);
		webClientBuilder.filter(new WebClientFilter());
	}
}
