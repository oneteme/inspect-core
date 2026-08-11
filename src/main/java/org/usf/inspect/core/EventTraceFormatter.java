package org.usf.inspect.core;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;

import java.time.Instant;
import java.util.stream.Stream;

/**
 * Builds string representations for trace events.
 */
public final class EventTraceFormatter {

	private String thread;
	private String action; //method
	private String user;
	private String resource;
	private String status;
	private Object result;
	private String period;
	
	/**
	 * Formats the configured trace parts as a single string.
	 *
	 * @return the formatted trace string
	 */
	public String format() {
		var sb = new StringBuilder(); 
		if(nonNull(action)) {
			sb.append('['+ action + ']').append(" ");
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
	
	/**
	 * Stores the thread label to include in the formatted output.
	 *
	 * @param thread the thread label
	 * @return this formatter
	 */
	public EventTraceFormatter withThread(String thread) {
		this.thread = thread;
		return this;
	}
	
	/**
	 * Stores the action name to include in the formatted output.
	 *
	 * @param action the action name
	 * @return this formatter
	 */
	public EventTraceFormatter withAction(String action) {
		this.action = action;
		return this;
	}

	/**
	 * Stores the user name to include in the formatted output.
	 *
	 * @param user the user name
	 * @return this formatter
	 */
	public EventTraceFormatter withUser(String user) {
		this.user = user;
		return this;
	}
	/**
	 * Stores a URL-like resource description to include in the formatted output.
	 *
	 * @param protocol the resource protocol
	 * @param host the resource host
	 * @param port the resource port
	 * @param path the resource path
	 * @param query the resource query string
	 * @return this formatter
	 */
	public EventTraceFormatter withUrlAsTopic(String protocol, String host, int port, String path, String query) {
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
	
	/**
	 * Stores a location or name to include in the formatted output.
	 *
	 * @param location the default location value
	 * @param name the preferred name value
	 * @return this formatter
	 */
	public EventTraceFormatter withLocationAsTopic(String location, String name) {
		this.resource = nonNull(name) ? name : location;
		return this;
	}
	
	/**
	 * Stores a message to include in the formatted output.
	 *
	 * @param message the message value
	 * @return this formatter
	 */
	public EventTraceFormatter withMessageAsTopic(String message) {
		this.resource = message;
		return this;
	}
	
	/**
	 * Stores a command and its arguments to include in the formatted output.
	 *
	 * @param command the command name
	 * @param args the command arguments
	 * @return this formatter
	 */
	public EventTraceFormatter withArgsAsTopic(String command, Object[] args) {
		this.resource = nonNull(command) ? command + " " : "";
		if(nonNull(args)) {
			this.resource += Stream.of(args)
			.map(c-> nonNull(c) ? c.toString() : "?")
			.collect(joining(", "));
		}
		return this;
	}

	/**
	 * Stores a status value to include in the formatted output.
	 *
	 * @param status the status value
	 * @return this formatter
	 */
	public EventTraceFormatter withStatus(String status) {
		this.status = status;
		return this;
	}

	/**
	 * Stores a result value to include in the formatted output.
	 *
	 * @param result the result value
	 * @return this formatter
	 */
	public EventTraceFormatter withResult(Object result) {
		this.result = result;
		return this;
	}

	/**
	 * Stores a single instant to include in the formatted output.
	 *
	 * @param instant the instant value
	 * @return this formatter
	 */
	public EventTraceFormatter withInstant(Instant instant) {
		if(nonNull(instant)){
			this.period = "(at " + instant + ")";
		}
		return this;
	}
	
	/**
	 * Stores a duration description based on the supplied start and end instants.
	 *
	 * @param start the start instant
	 * @param end the end instant
	 * @return this formatter
	 */
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
