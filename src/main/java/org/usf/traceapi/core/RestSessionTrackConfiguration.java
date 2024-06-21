package org.usf.traceapi.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.UnaryOperator.identity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public final class RestSessionTrackConfiguration {
	
	private static final String[] EMPTY  = new String[0];
	private static final String METH_KEY = "method";
	private static final String PATH_KEY = "path";

	private Map<String, String[]> excludes; //method, path 
	
	public String[] excludedMethods() {
		return excludes.get(METH_KEY);
	}
	
	public String[] excludedPaths() {
		return excludes.get(PATH_KEY);
	}
	
	static String[] assertContainsNoEmpty(String[] arr, UnaryOperator<String> fn) {
		if(isNull(arr)) {
			return EMPTY;
		}
		for(int i=0; i<arr.length; i++) {
			var s = arr[i];
			if(nonNull(s) && !s.isBlank()) {
				arr[i] = fn.apply(arr[i]);
			}
			else {
				throw new IllegalArgumentException("illegal values : " + Arrays.toString(arr));
			}
		}
		return arr;
	}

	void validate() {
		if(isNull(excludes)) {
			excludes = new HashMap<>(2);
		}
		excludes.compute(METH_KEY, (key,arr)-> assertContainsNoEmpty(arr, String::toUpperCase));
		excludes.compute(PATH_KEY, (key,arr)-> assertContainsNoEmpty(arr, identity()));
	}
}
