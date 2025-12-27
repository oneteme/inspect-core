package org.usf.inspect.http;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.BeanUtils.logRegistringBean;
import static org.usf.inspect.core.ScheduledExecutorServiceWrapper.wrap;
import static org.usf.inspect.core.SessionContextManager.activeContext;
import static reactor.core.publisher.Hooks.onLastOperator;
import static reactor.core.publisher.Operators.lift;
import static reactor.core.scheduler.Schedulers.onScheduleHook;
import static reactor.core.scheduler.Schedulers.setExecutorServiceDecorator;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.usf.inspect.core.SessionContextManager;

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
		setExecutorServiceDecorator("inspect-executor-decorator", (sc,es)-> wrap(es, "ReactorExecutorService")); //for Schedulers.fromExecutorService(...), Mono.toFuture()
		onScheduleHook("inspect-schedule-hook", SessionContextManager::aroundRunnable); //for Schedulers.parallel(), single(), boundedElastic()
		onLastOperator("inspect-operator-hook", p-> {
 			var ses = activeContext();
 			return nonNull(ses) ? lift((scn,sub)-> new CoreSubscriberProxy<>(sub, ses)).apply(p) : p;
 		}); //for Mono.defer, Flux.defer, ...
	}

    @Bean
    @DependsOn("inspectHub") //ensure inspectHub is loaded first
	WebClientCustomizer webClientCustomizer() {
		return wcb->{
			logRegistringBean("webClientFilter", WebClientFilter.class);
			wcb.filter(new WebClientFilter());
		};
	}
}
