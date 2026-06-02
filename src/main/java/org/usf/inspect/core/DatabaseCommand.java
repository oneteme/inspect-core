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
		var len = sql.length();
		var idx = nextToken(sql, -1);
    	if(sql.regionMatches(true, idx, "WITH ", 0, 5)) {
    		idx = nextChar(sql, idx+5, '(');
			var prth = 1;
			char c = 0;
			while((idx= nextToken(sql, idx)) < len) {
				c = sql.charAt(idx);
				if(c == '(') {
					++prth;
				}
				else if(c == ')' && --prth == 0) {
		    		idx = nextToken(sql, idx);
		    		if(sql.charAt(idx) == ',') {
		    			idx = nextChar(sql, idx, '(');
	    				prth = 1;
		    		}
		    		else {
		    			break;
		    		}
				}
			}
    	}
    	DatabaseCommand main = null;
		if(idx < len) {
			do {
	    		DatabaseCommand cmd = null;
	    		for(var c : values()) { //exclude SQL !
	    			var s = c.name();
	    			var cLen = s.length();
			        if(sql.regionMatches(true, idx, s, 0, cLen) && idx+cLen<len && isWhitespacePlus(sql.charAt(idx+cLen))) {
			        	cmd = c;
			        	idx+= cLen+1;
			        	break;
			        }
	    		}
		        main = mergeCommand(main, cmd);	        
	    	} while(nonNull(main) && main != SQL && (idx=nextToken(sql, nextChar(sql, idx, ';'))) < len);
		}
        return main;
    }
	
	static int nextChar(String s, int idx, char to) {
		var len = s.length();
		var qot = false;
		while((idx=skipComment(s, idx)) < len) {
			char c = s.charAt(idx);
			if (qot) {
	            if (c=='\\' && idx+1<len) {
	            	++idx; //skip escaped char
	            }
	            else if (c=='\'') {
	                if (idx+1<len && s.charAt(idx+1)=='\'') {
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
	
	static int nextToken(String s, int idx) {
		var len = s.length();
		do {
			idx++;
		}while((idx=skipComment(s,idx))<len && isWhitespacePlus(s.charAt(idx)));
		return idx;
	}
	
	static int skipComment(String s, int idx) {
		var len = s.length();
		if(idx+1 < len) {
			var c = s.charAt(idx);
			if(c=='-' && s.charAt(idx+1)=='-') {
				idx+=2;
				while(idx<len && s.charAt(idx)!='\n') {
					++idx;
				}
				if(idx<len) { //skip \n
					++idx;
				}
			}
		}
		return idx;
	}
	
	static boolean isWhitespacePlus(char c) {
		return isWhitespace(c) || c == ' ';
	}
	
	static DatabaseCommand mergeCommand(DatabaseCommand main, DatabaseCommand cmd) {
		if(main == cmd || isNull(cmd)) {
			return main;
		}
		return isNull(main) ? cmd : SQL;
	}
}