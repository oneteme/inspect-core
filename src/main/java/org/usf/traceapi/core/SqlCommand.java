package org.usf.traceapi.core;

import static java.nio.CharBuffer.wrap;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.MULTILINE;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;

/**
 * 
 * @author u$f
 *
 */
public enum SqlCommand {
	
	CREATE, DROP, ALTER, TRUNCATE, //DDL
	GRANT, REVOKE, //DCL
	INSERT, UPDATE, DELETE, //DML
	SELECT, //DQL
	SQL;
	
	public static final Pattern PATTERN =
			compile(Stream.of(values())
			.filter(c-> c != SQL) // not a command
			.map(Object::toString)
			.collect(joining("|", "^[\n\t\s]*(", ")[\n\t\s]*"))
			, MULTILINE & CASE_INSENSITIVE);

	public static final Pattern WITH_PATTERN =
			compile("^[\n\t\s]*WITH[\n\t\s]*"
					, MULTILINE & CASE_INSENSITIVE);
	
	public static final Pattern SQL_PATTERN = 
			compile(".+;.*\\w+", DOTALL);

	public static SqlCommand mainCommand(@Nonnull String query){
		if(SQL_PATTERN.matcher(query).find()) {
			return SQL;
		}
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
