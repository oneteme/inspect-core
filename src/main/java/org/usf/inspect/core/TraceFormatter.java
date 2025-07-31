package org.usf.inspect.core;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;

import java.time.Instant;
import java.util.stream.Stream;

public final class TraceFormatter {

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
	
	public TraceFormatter withCommand(String command) {
		this.command = command;
		return this;
	}

	public TraceFormatter withUser(String user) {
		this.user = user;
		return this;
	}
	public TraceFormatter withUrlAsResource(String protocol, String host, int port, String path, String query) {
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
//			if(!path.startsWith("/") && !s.endsWith("/")) { //host & port are null
//				s+= '/';
//			}
			sb.append(path);
		}
		if(nonNull(query)) {
			sb.append("?"+ query);
		}
		this.resource=sb.toString();
		return this;
	}
	
	public TraceFormatter withLocationAsResource(String classname, String methodName) {
		this.resource = nonNull(classname) ? classname : "";
		if(nonNull(methodName)) {
			this.resource+= "." + methodName;
		}
		return this;
	}
	
	public TraceFormatter withMessageAsResource(String message) {
		this.resource = message;
		return this;
	}
	
	public TraceFormatter withArgsAsResource(Object[] args) {
		if(nonNull(args)) {
			this.resource = Stream.of(args)
			.map(c-> nonNull(c) ? c.toString() : "?")
			.collect(joining(", "));
		}
		return this;
	}
	
	public TraceFormatter withThread(String thread) {
		this.thread = thread;
		return this;
	}

	public TraceFormatter withStatus(String status) {
		this.status = status;
		return this;
	}

	public TraceFormatter withResult(Object result) {
		this.result = result;
		return this;
	}
	

	public TraceFormatter withInstant(Instant instant) {
		if(nonNull(instant)){
			this.period = "(at " + instant + ")";
		}
		return this;
	}
	
	public TraceFormatter withPeriod(Instant start, Instant end) {
		if(nonNull(start) && nonNull(end)) {
			this.period = "(in " +  start.until(end, MILLIS) + "ms)";
		}
		else if(nonNull(start)) {
			this.period = "(pending..)";
		}
		return this;
	}
}
