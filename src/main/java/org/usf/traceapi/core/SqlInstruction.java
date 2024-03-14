package org.usf.traceapi.core;

import static java.nio.CharBuffer.wrap;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.MULTILINE;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 
 * @author u$f
 *
 */
public enum SqlInstruction {
	
	CREATE, DROP, ALTER, TRUNCATE, //DDL
	GRANT, REVOKE, //DCL
	INSERT, UPDATE, DELETE, //DML
	SELECT; //DQL
	
	public static final Pattern PATTERN =
			compile(Stream.of(values())
			.map(SqlInstruction::toString)
			.collect(joining("|", "^[\n\t\s]*(", ")[\n\t\s]*"))
			, MULTILINE & CASE_INSENSITIVE);

	public static final Pattern WITH_PATTERN =
			compile("^[\n\t\s]*WITH[\n\t\s]*"
					, MULTILINE & CASE_INSENSITIVE);

	public static SqlInstruction extractInstruction(String query){
		var m = WITH_PATTERN.matcher(query);
		var idx = m.find() ? jumpParentheses(query, m.end()) : 0;
		var s = idx == 0 ? query : wrap(query).subSequence(idx, query.length());
		m = PATTERN.matcher(s);
		return m.find() ? valueOf(m.group(1)) : null;
	}
	
	private static int jumpParentheses(String query, int from) {
		var beg = query.indexOf('(', from);
		if(beg > -1) {
			var par = 1;
			for(var i=beg+1; i<query.length(); i++) {
				if(query.charAt(i) == '(') {
					par++;
				}
				else if(query.charAt(i) == ')') {
					par--;
					if(par == 0) {
						return ++i;
					}
				}
			}
		}
		return from;
	}
}
