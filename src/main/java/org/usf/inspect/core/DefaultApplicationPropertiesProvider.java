package org.usf.inspect.core;

import static java.lang.String.join;
import static java.lang.System.getProperty;
import static java.lang.System.getenv;
import static java.net.InetAddress.getLocalHost;
import static java.time.Clock.systemDefaultZone;
import static java.util.Locale.getDefault;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringBootVersion;
import org.springframework.core.SpringVersion;
import org.springframework.core.env.Environment;

import jakarta.servlet.ServletContext;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
public class DefaultApplicationPropertiesProvider implements ApplicationPropertiesProvider {
	
	@NonNull
	private final Environment env;

	@Override
	public String getName() {
		return getEnvironmentProperty("name");
	}

	@Override
	public String getVersion() {
		return getEnvironmentProperty("version");
	}

	@Override
	public String getBranch() {
		return getEnvironmentProperty("branch");
	}

	@Override
	public String getCommitHash() {
		return getEnvironmentProperty("hash");
	}
	
	private String getEnvironmentProperty(String p) {
		return env.getProperty("spring.application." + p);
	}

	@Override
	public String getEnvironment() {
		var envs = env.getActiveProfiles();
		return nonNull(envs) ? join(",", envs) : null;
	}
	
	@Override
	public Map<String, String> additionalProperties(ServletContext servletContext) {
		var meta = new HashMap<String, String>();
		meta.put("user.locale", getDefault().getLanguage() + '~' + systemDefaultZone().getZone());
		meta.put("host.name", hostName());
		meta.put("server.info", servletContext.getServerInfo());
        meta.put("servlet.version", servletContext.getMajorVersion() + "." + servletContext.getMinorVersion());
        meta.put("java.runtime.name", getProperty("java.runtime.name"));
        meta.put("spring.core.version", SpringVersion.getVersion());
        meta.put("spring.boot.version", SpringBootVersion.getVersion());
        meta.put("process.pid", String.valueOf(ProcessHandle.current().pid()));
        return meta;
	}

	static String hostName() {
		var name = getenv("HOSTNAME");
		if(isNull(name)) {
			try {
				name = getLocalHost().getHostName(); //hostName 
			} catch (UnknownHostException e) {
				//do nothing
			}
		}
		return name;
	}
}
