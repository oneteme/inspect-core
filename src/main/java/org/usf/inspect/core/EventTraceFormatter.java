package org.usf.inspect.core;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;

import java.time.Instant;
import java.util.stream.Stream;

public final class EventTraceFormatter {

	private String thread;
	private String command; //method
	private String user;
	private String resource;
	private String status;
	private Object result;
	private String period;
	
	public String format() {
		var sb = new StringBuilder(); 
		if(nonNull(command)) {
			sb.append('['+ command + ']').append(" ");
		}
		if(nonNull(user)) {
			sb.append(user+"@");
		}
		if(nonNull(resource)) {
			sb.append(resource);
		}
		if(nonNull(status) || nonNull(result)) {
			sb.append(" >> ");
			if(nonNull(status)) {
				sb.append(status);
			}
			if(nonNull(result)) {
				sb.append('{'+result.toString()+'}');
			}
		}
		if(nonNull(period)) {
			sb.append(" " + period);
		}
		if(nonNull(thread)) {
			sb.append(" ~" + thread);
		}
		return sb.toString();
	}
	
	public EventTraceFormatter withThread(String thread) {
		this.thread = thread;
		return this;
	}
	
	public EventTraceFormatter withCommand(String command) {
		this.command = command;
		return this;
	}

	public EventTraceFormatter withUser(String user) {
		this.user = user;
		return this;
	}
	public EventTraceFormatter withUrlAsResource(String protocol, String host, int port, String path, String query) {
		var sb = new StringBuilder();
		if(nonNull(protocol)) {
			sb.append(protocol + "://");
		}
		if(nonNull(host)) {
			sb.append(host);
		}
		if(port > 0) {
			sb.append(":"+port);
		}
		if(nonNull(path)) {
			if(!path.startsWith("/")) { //host & port are null
				sb.append('/');
			}
			sb.append(path);
		}
		if(nonNull(query)) {
			sb.append("?"+ query);
		}
		this.resource=sb.toString();
		return this;
	}
	
	public EventTraceFormatter withLocationAsResource(String location, String name) {
		this.resource = nonNull(name) ? name : location;
		return this;
	}
	
	public EventTraceFormatter withMessageAsResource(String message) {
		this.resource = message;
		return this;
	}
	
	public EventTraceFormatter withArgsAsResource(Object[] args) {
		if(nonNull(args)) {
			this.resource = Stream.of(args)
			.map(c-> nonNull(c) ? c.toString() : "?")
			.collect(joining(", "));
		}
		return this;
	}

	public EventTraceFormatter withStatus(String status) {
		this.status = status;
		return this;
	}

	public EventTraceFormatter withResult(Object result) {
		this.result = result;
		return this;
	}

	public EventTraceFormatter withInstant(Instant instant) {
		if(nonNull(instant)){
			this.period = "(at " + instant + ")";
		}
		return this;
	}
	
	public EventTraceFormatter withPeriod(Instant start, Instant end) {
		if(nonNull(start) && nonNull(end)) {
			this.period = "(in " +  start.until(end, MILLIS) + "ms)";
		}
		else if(nonNull(start)) {
			this.period = "(pending..)";
		}
		return this;
	}
}
