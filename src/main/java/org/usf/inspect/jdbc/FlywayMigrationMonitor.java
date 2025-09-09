package org.usf.inspect.jdbc;

import static java.time.Instant.now;
import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.LocalRequestType.EXEC;
import static org.usf.inspect.core.SessionManager.createLocalRequest;

import org.flywaydb.core.Flyway;
import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;
import org.usf.inspect.core.LocalRequest;


public final class FlywayMigrationMonitor {
	
	private final LocalRequest req = createLocalRequest();

	public ExecutionHandler<Void> migrationHandler(Flyway fly) {
		call(()->{
			req.setStart(now());
			req.setThreadName(threadName());
			req.setType(EXEC.name());
			req.setName("migration");
			req.setLocation(nonNull(fly.getConfiguration().getLocations())
					? stream(fly.getConfiguration().getLocations()).map(Object::toString).collect(joining(","))
							: Flyway.class.getName() + ".migrate()");
			req.setUser(fly.getConfiguration().getUser());
			return req;
		});
		return (s,e,v,t)->{
			req.runSynchronized(()-> {
				if(nonNull(t)) {
					req.setException(mainCauseException(t));
				}
				req.setEnd(now());
			});
			return req;
		};
	}
}
