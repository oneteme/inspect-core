package org.usf.inspect.core;
 
import static java.lang.Character.isWhitespace;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.EDIT;
import static org.usf.inspect.core.CommandType.EMIT;
import static org.usf.inspect.core.CommandType.READ;
import static org.usf.inspect.core.CommandType.ROLE;
import static org.usf.inspect.core.CommandType.SCRIPT;
import static org.usf.inspect.core.CommandType.SETUP;

import lombok.Getter;
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
	INSERT(EMIT), UPDATE(EDIT), DELETE(EDIT), MERGE(EDIT), //DML
	SELECT(READ), //DQL
	//TCL 
	SET(null), GET(null), //OTHER
	CALL(SCRIPT), SQL(SCRIPT); //multiple command
	
	private final CommandType type;

	public static DatabaseCommand extractCommand(String sql) {
		if(isNull(sql) || sql.isBlank()) {
			return null;
		}
		var idx = skipWhiteSpace(sql, 0);
		var len = sql.length();
    	if(sql.regionMatches(true, idx, "WITH ", 0, 5)) {
    		idx = jumpTo(sql, idx+5, '(');
			var prth = 0;
			char c = 0;
			for(; idx < len; idx++) {
				c = sql.charAt(idx);
				if(c == '(') {
					++prth;
				}
				else if(c == ')' && --prth == 0) {
		    		idx = skipWhiteSpace(sql, ++idx);
		    		if(sql.charAt(idx) == ',') {
		    			idx = jumpTo(sql, idx, '(');
	    				prth = 1;
		    		}
		    		else {
		    			break;
		    		}
				}
				idx = skipComment(sql, idx);
			}
    		idx = skipWhiteSpace(sql, idx);
    	}
    	DatabaseCommand main = null;
		if(idx < len) {
			do {
	    		DatabaseCommand cmd = null;
	    		for(var c : values()) { //exclude SQL !
	    			var s = c.name();
	    			var cLen = s.length();
			        if(sql.regionMatches(true, idx, s, 0, cLen) && idx+cLen<len && isWhitespace(sql.charAt(idx+cLen))) {
			        	cmd = c;
			        	break;
			        }
	    		}
		        main = mergeCommand(main, cmd);	        
	    	} while(nonNull(main) && main != SQL && (idx=skipWhiteSpace(sql, jumpTo(sql, idx, ';')+1)) < sql.length());
		}
        return main;
    }
	
	static int jumpTo(String s, int idx, char to) {
		var len =  s.length();
		var qot = false;
		while(idx < len) {
			idx = skipComment(s, idx);
			char c = s.charAt(idx);
			if (qot) {
	            if (c=='\\' && idx+1<len) {
	            	++idx; //skip escaped char
	            }
	            else if (c=='\'') {
	                if (idx+1<len && s.charAt(idx + 1)=='\'') { // double quotes
	                	++idx; // skip escaped quote
	                }
	                else {
	                	qot = false; 
	                }
	            }
	        }
			else if (c == '\'') {
	            qot = true;
	        }
			else if(c == to) {
				break;
			}
			++idx;
		}
		return idx;
	}
	
	static int skipWhiteSpace(String s, int idx) {
		while(idx<s.length() && isWhitespace(s.charAt(idx))) {
			idx++;
		}
		return idx;
	}
	
	static int skipComment(String s, int idx) {
		var len =  s.length();
		var c = s.charAt(idx);
		if(c=='-' && idx+1<len && s.charAt(idx+1)=='-') {
			idx+=2;
			while(idx<len && s.charAt(idx)!='\n') {
				++idx;
			}
		}
		return idx; //return index of \n
	}
	
	static DatabaseCommand mergeCommand(DatabaseCommand main, DatabaseCommand cmd) {
		if(main == cmd || isNull(cmd)) {
			return main;
		}
		return isNull(main) ? cmd : SQL;
	}
	
	public static void main(String[] args) {
		System.out.println(extractCommand(v));
	}
	
	static String v = """
		WITH -- DUMMY COMMENT )'(
		cte AS -- DUMMY COMMENT )'(
		(SELECT -- DUMMY COMMENT )'(
		id FROM -- DUMMY COMMENT )'(
		(SELECT -- DUMMY COMMENT )'(
		id FROM -- DUMMY COMMENT )'(
		users) x) SELECT -- DUMMY COMMENT )'(
		* FROM -- DUMMY COMMENT )'(
		cte;
	""";
}