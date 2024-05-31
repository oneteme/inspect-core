package org.usf.traceapi.core;

import static java.lang.String.format;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class SessionStage extends Stage implements MutableStage {
	
//	private String name; //method, title
	private String location; //class, URL
	private String user;
	private String threadName;
	
	@Override
	public String toString() {
		return format("%-25s", threadName) + ": " 
				+ this.getClass().getSimpleName() 
				+ " {" +  format("%5s", duration()) + "ms}";
	}
}