package org.usf.traceapi.core;

import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.UnaryOperator.identity;

import java.util.Arrays;
import java.util.Map;
import java.util.function.UnaryOperator;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@ToString
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

	void validate() {
		if(isNull(excludes)) {
			excludes = emptyMap();
		}
		else {
			excludes.computeIfPresent(METH_KEY, (key,arr)-> assertAllNonEmpty(arr, String::toUpperCase));
			excludes.computeIfPresent(PATH_KEY, (key,arr)-> assertAllNonEmpty(arr, identity()));
		}
	}
	
	static String[] assertAllNonEmpty(String[] arr, UnaryOperator<String> fn) {
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
}
