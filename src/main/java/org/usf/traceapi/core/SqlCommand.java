package org.usf.traceapi.core;

import static java.nio.CharBuffer.wrap;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.MULTILINE;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import lombok.NonNull;

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
	SQL; //multiple command
	
	public static final Pattern PATTERN =
			compile(Stream.of(values())
			.filter(c-> c != SQL) // not a command
			.map(Object::toString)
			.collect(joining("|", "^\s*(", ")\s*"))
			, MULTILINE | CASE_INSENSITIVE);

	public static final Pattern WITH_PATTERN =
			compile("^\s*WITH\s+\\w+\s+AS\s*", MULTILINE | CASE_INSENSITIVE);
	
	public static final Pattern SQL_PATTERN = 
			compile(".+;.*\\w+", DOTALL);

	public static SqlCommand mainCommand(@NonNull String query){
		if(SQL_PATTERN.matcher(query).find()) { //multiple 
			return SQL;
		}
		var m = WITH_PATTERN.matcher(query); //TD multiple !?
		var idx = m.find() ? jumpParentheses(query, m.end()) : 0;
		var s = idx == 0 ? query : wrap(query).subSequence(idx, query.length());
		m = PATTERN.matcher(s);
		return m.find() ? valueOf(m.group(1).toUpperCase()) : null;
	}
	
	private static int jumpParentheses(String query, int from) {
		var par = 0;
		var beg = from;
		for(var i=beg; i<query.length(); i++) {
			if(query.charAt(i) == '(') {
				par++;
			}
			else if(query.charAt(i) == ')') {
				par--;
				if(par == 0) {
					return ++i;
				}
				else if(par < 0) {
					break; //bad query
				}
			}
		}
		return from;
	}
}
