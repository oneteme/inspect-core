package org.usf.inspect.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public final class ExceptionConfiguration {
	
	private int maxStackTrace = 5;
	private int messageMaxLength = 1000; // max length of exception message, 0 means no limit
}
