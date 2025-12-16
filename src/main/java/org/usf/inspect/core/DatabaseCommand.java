package org.usf.inspect.core;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.MULTILINE;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;
import static org.usf.inspect.core.CommandType.EDIT;
import static org.usf.inspect.core.CommandType.EMIT;
import static org.usf.inspect.core.CommandType.READ;
import static org.usf.inspect.core.CommandType.ROLE;
import static org.usf.inspect.core.CommandType.SCRIPT;
import static org.usf.inspect.core.CommandType.SETUP;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@Getter
@RequiredArgsConstructor
public enum DatabaseCommand {
	
	CREATE(SETUP), DROP(SETUP), ALTER(SETUP), TRUNCATE(SETUP), //DDL
	GRANT(ROLE), REVOKE(ROLE), //DCL
	INSERT(EMIT), UPDATE(EDIT), DELETE(EDIT), //DML
	SELECT(READ), //DQL
	//TCL 
	SET(null), GET(null), //OTHER
	CALL(SCRIPT), SQL(SCRIPT); //multiple command
	
	private final CommandType type;
	
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

	public static DatabaseCommand parseCommand(@NonNull String query){
		if(SQL_PATTERN.matcher(query).find()) { //multiple 
			return SQL;
		}
		var idx = skipWithClause(query);
		var m = PATTERN.matcher(query).region(idx, query.length());
		return m.find() ? valueOf(m.group(1).toUpperCase()) : null;
	}
	
	private static int skipWithClause(String s) {
		var idx = 0;
		var m = WITH_PATTERN.matcher(s);
		if(m.find()) {
			var p = compile("^\s*,\s*\\w+\s+AS\s*", MULTILINE | CASE_INSENSITIVE); //multiple
			do {
				idx = jumpParentheses(s, m.end());
				if(idx == m.end()) {
					log.warn("'(' expected at {} after WITH clause : {}", idx, s);
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
					log.warn("unexpected character ')' at {} : {}", i, query);
					break; //bad query
				}
			}
		}
		return from;
	}
}
