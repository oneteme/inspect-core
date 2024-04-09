package org.usf.traceapi.core;

import static java.nio.CharBuffer.wrap;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.MULTILINE;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;
import static org.usf.traceapi.core.Helper.log;

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
		var idx = skipWithClause(query);
		var m = PATTERN.matcher(query).region(idx, query.length());
		return m.find() ? valueOf(m.group(1).toUpperCase()) : null;
	}
	
	private static int skipWithClause(String s) {
		var m = WITH_PATTERN.matcher(s); //TD multiple !?
		int idx = 0;
		if(m.find()) {
			var p = compile("^\s*\\,\s*", MULTILINE);
			do {
				idx = jumpParentheses(s, m.end());
				if(idx == m.end()) {
					break;
				}
				m = p.matcher(s).region(idx, s.length());
			} while(m.find());
		}
		return idx;
	}
	
	private static int jumpParentheses(CharSequence query, int from) {
		var deep = 0;
		for(var i=from; i<query.length(); i++) {
			if(query.charAt(i) == '(') {
				deep++;
			}
			else if(query.charAt(i) == ')') {
				deep--;
				if(deep == 0) {
					return ++i;
				}
				else if(deep < 0) {
					log.warn("dirty query : {}", query);
					break; //bad query
				}
			}
		}
		return from;
	}
}
