package org.usf.traceapi.core;

import static java.lang.String.format;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class RunnableStage implements Metric, MutableStage {
	
	private String name; //method, title
	private String location; //class, URL
	private Instant start;
	private Instant end;
	private String user;
	private String threadName;
	private ExceptionInfo exception;
	
	@Override
	public String toString() {
		return format("%-25s", threadName) + ": " 
				+ this.getClass().getSimpleName() 
				+ " {" +  format("%5s", duration()) + "ms}";
	}
}