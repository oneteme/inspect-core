package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.BeanUtils.logLoadingBean;
import static org.usf.inspect.core.Constants.INSPECT_ENABLED;
import static org.usf.inspect.core.Constants.REPORTER_NAME;
import static org.usf.inspect.core.Constants.SYSTEM_ENABLED;
import static org.usf.inspect.core.TraceHub.hub;
import static org.usf.inspect.slf4j.InspectLogReporter.configureLogger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.usf.inspect.InspectCollectorProperties;
import org.usf.inspect.RestRemoteServerProperties;
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
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@ConditionalOnProperty(name = INSPECT_ENABLED, havingValue = "true", matchIfMissing = false) 
public class InspectConfiguration {
	
	static InspectConfiguration INSTANCE = new InspectConfiguration(new InspectCollectorProperties(), new StandardEnvironment());

	@Getter
	private final InspectCollectorProperties properties;
	private final Environment environment;
	
	@Autowired
	InspectConfiguration(Environment environment) {
		this.environment = environment;
		this.properties = loadConfiguration(environment, "inspect.collector");
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
	
	static InspectCollectorProperties loadConfiguration(Environment env, String prefix) {
		if(isNull(env) || !"true".equals(env.getProperty(prefix+".enabled"))) {
			return new InspectCollectorProperties();
		}
		log.info("loading '{}' configuration from environment", prefix);
		var bind = Binder.get(env);
        var conf = bind.bind(prefix, InspectCollectorProperties.class)	
        	.orElseThrow(()-> new IllegalStateException("cannot bind '%s' configuration".formatted(prefix)));
        var mode = conf.getTracing().getType();
        if(nonNull(mode)) {
        	var publisher = switch (mode) {
        	case REST -> bind.bind(prefix+".tracing.remote", RestRemoteServerProperties.class)
        		.orElseThrow(()-> new IllegalStateException("cannot bind '%s.tracing.remote' configuration".formatted(prefix)));
        	default -> throw new UnsupportedOperationException("unsupported publisher type: " + mode);
        	};
        	conf.getTracing().setRemote(publisher);
        }
        conf.validate();
        return conf;
	}
}
