package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.BeanUtils.logLoadingBean;
import static org.usf.inspect.core.Constants.INSPECT_ENABLED;
import static org.usf.inspect.core.Constants.REPORTER_NAME;
import static org.usf.inspect.core.Constants.SYSTEM_ENABLED;
import static org.usf.inspect.core.TraceHub.hub;
import static org.usf.inspect.slf4j.InspectLogReporter.configureLogger;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.usf.inspect.config.InspectCollectorProperties;
import org.usf.inspect.config.RestRemoteServerProperties;
import org.usf.inspect.slf4j.InspectLogReporter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@Configuration
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@ConditionalOnProperty(name = INSPECT_ENABLED, havingValue = "true", matchIfMissing = false) 
public class InspectConfiguration {
	
	static InspectConfiguration INSTANCE = new InspectConfiguration(new InspectCollectorProperties(), new StandardEnvironment());

	@Getter
	private final InspectCollectorProperties properties;
	private final Environment environment;
	
	InspectConfiguration(Environment environment) {
		this.environment = environment;
		this.properties = loadConfiguration(environment);
		var report = properties.getReporting();
		if(report.isEnabled()) {
			configureLogger(REPORTER_NAME, false, ()-> new InspectLogReporter(hub(), report.isStackTrace()));
		}
		else {
			configureLogger(REPORTER_NAME, false);
		}
	}
	
	@Bean //hook must be loaded before hubDispatcher
	@ConditionalOnProperty(name = SYSTEM_ENABLED, havingValue = "true", matchIfMissing = true)
	SystemMonitor systemMonitor() {
		logLoadingBean("systemMonitor", SystemMonitor.class);
		return new SystemMonitor(properties.getMonitoring().getSystem());
	}

	public boolean isEnabled() {
		return properties.isEnabled();
	}
	
	public boolean isEnabled(String key, boolean matchIfMissing) {
		return properties.isEnabled() && environment.getProperty(key, boolean.class, matchIfMissing);
	}
	
	public static InspectConfiguration getConfiguration() {
		return INSTANCE;
	}
	
	static InspectCollectorProperties loadConfiguration(Environment env) {
		if(isNull(env) || !"true".equals(env.getProperty("inspect.collector.enabled"))) {
			return new InspectCollectorProperties();
		}
		log.info("loading 'inspect.collector' configuration from environment");
		var bind = Binder.get(env);
        var conf = bind.bind("inspect.collector", InspectCollectorProperties.class)	
        	.orElseThrow(()-> new IllegalStateException("cannot bind 'inspect.collector' configuration"));
        var mode = conf.getTracing().getType();
        if(nonNull(mode)) {
        	var publisher = switch (mode) {
        	case REST -> bind.bind("inspect.collector.tracing.remote", RestRemoteServerProperties.class)
        		.orElseThrow(()-> new IllegalStateException("cannot bind 'inspect.collector.tracing.remote' configuration"));
        	default -> throw new UnsupportedOperationException("unsupported publisher type: " + mode);
        	};
        	conf.getTracing().setRemote(publisher);
        }
        conf.validate();
        return conf;
	}
}
