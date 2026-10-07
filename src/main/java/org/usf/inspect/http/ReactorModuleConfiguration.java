package org.usf.inspect.http;

import static org.usf.inspect.core.BeanUtils.logRegistringBean;
import static org.usf.inspect.core.ScheduledExecutorServiceWrapper.wrap;
import static reactor.core.publisher.Hooks.onEachOperator;
import static reactor.core.publisher.Hooks.resetOnEachOperator;
import static reactor.core.publisher.Operators.lift;
import static reactor.core.scheduler.Schedulers.onScheduleHook;
import static reactor.core.scheduler.Schedulers.removeExecutorServiceDecorator;
import static reactor.core.scheduler.Schedulers.resetOnScheduleHook;
import static reactor.core.scheduler.Schedulers.setExecutorServiceDecorator;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.usf.inspect.core.ContextPropagators;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnClass(name = "reactor.core.publisher.Hooks")
@ConditionalOnProperty(prefix = "inspect.collector", name = "enabled", havingValue = "true")
public class ReactorModuleConfiguration implements InitializingBean, DisposableBean {

	static final String HOOK_KEY = "inspect-context-propagation";

	@Override
	public void afterPropertiesSet() {
		setExecutorServiceDecorator(HOOK_KEY, (sc, es)-> wrap(es, true, "ReactorExecutorService"));
		log.debug("registering reactor hooks '{}'", HOOK_KEY);
		onScheduleHook(HOOK_KEY, ContextPropagators::wrapRunnable);
		onEachOperator(HOOK_KEY, lift(CoreSubscriberProxy::lift));
	}

	@Override
	public void destroy() {
		log.debug("removing reactor hooks '{}'", HOOK_KEY);
		resetOnEachOperator(HOOK_KEY);
		resetOnScheduleHook(HOOK_KEY);
		removeExecutorServiceDecorator(HOOK_KEY);
	}

	@Bean
	@ConditionalOnClass(name = "org.springframework.web.reactive.function.client.ExchangeFilterFunction")
	WebClientCustomizer webClientCustomizer() {
		return wcb-> {
			logRegistringBean("webClientFilter", WebClientFilter.class);
			wcb.filter(new WebClientFilter());
		};
	}
}