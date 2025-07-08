package org.usf.inspect.core;

import static java.lang.String.join;
import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;

import java.util.Collections;
import java.util.Map;

import org.springframework.core.env.Environment;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class DefaultApplicationPropertiesProvider implements ApplicationPropertiesProvider {

	@NonNull
	private final Environment env;

	@Override
	public String getName() {
		return getProperty("name");
	}

	@Override
	public String getVersion() {
		return getProperty("version");
	}

	@Override
	public String getBranch() {
		return getProperty("branch");
	}

	@Override
	public String getCommitHash() {
		return getProperty("hash");
	}
	
	private String getProperty(String p) {
		return env.getProperty("spring.application." + p);
	}

	@Override
	public String getEnvironment() {
		var envs = env.getActiveProfiles();
		return isNull(envs) ? null : join(",", envs);
	}
	
	@Override
	public Map<String, String> additionalProperties() {
		return emptyMap();
	}
}
