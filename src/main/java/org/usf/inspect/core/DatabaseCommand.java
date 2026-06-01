package org.usf.inspect.core;
 
import static java.lang.Character.isWhitespace;
import static java.util.Objects.isNull;
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
    	if(sql.regionMatches(true, idx, "WITH ", 0, 5)) {
    		idx = jumpTo(sql, idx, '(')+1;
			var prth = 1;
			char c = 0;
			for(; idx < sql.length(); idx++) {
				c = sql.charAt(idx);
				if(c == '(') {
					++prth;
				}
				if(c == ')' && --prth == 0) {
		    		idx = skipWhiteSpace(sql, ++idx);
		    		if(sql.charAt(idx) == ',') {
		    			idx = jumpTo(sql, idx, '(');
	    				prth = 1;
		    		}
		    		else {
		    			break;
		    		}
				}
			}
    		idx = skipWhiteSpace(sql, idx);
    	}
    	DatabaseCommand main = null;
		if(idx < sql.length()) {
			do {
	    		DatabaseCommand cmd = null;
		        if(sql.regionMatches(true, idx, "SELECT ", 0, 7)) {
		        	cmd = SELECT;
		        }
		        else if(sql.regionMatches(true, idx, "INSERT ", 0, 7)) {
		        	cmd = INSERT;
		        }
		        else if(sql.regionMatches(true, idx, "DELETE ", 0, 7)) {
		        	cmd = DELETE;
		        }
		        else if(sql.regionMatches(true, idx, "UPDATE ", 0, 7)) {
		        	cmd = UPDATE;
		        }
		        else if(sql.regionMatches(true, idx, "MERGE ", 0, 6)) {
		        	cmd = MERGE;
		        }
		        else if(sql.regionMatches(true, idx, "CREATE ", 0, 7)) {
		        	cmd = CREATE;
		        }
		        else if(sql.regionMatches(true, idx, "DROP ", 0, 5)) {
		        	cmd = DROP;
		        }
		        else if(sql.regionMatches(true, idx, "ALTER ", 0, 6)) {
		        	cmd = ALTER;
		        }
		        else if(sql.regionMatches(true, idx, "TRUNCATE ", 0, 9)) {
		        	cmd = TRUNCATE;
		        }
		        else if(sql.regionMatches(true, idx, "GRANT ", 0, 6)) {
		        	cmd = GRANT;
		        }
		        else if(sql.regionMatches(true, idx, "REVOKE ", 0, 5)) {
		        	cmd = REVOKE;
		        }
		        else if(sql.regionMatches(true, idx, "GET ", 0, 4)) {
		        	cmd = GET;
		        }
		        else if(sql.regionMatches(true, idx, "SET ", 0, 4)) {
		        	cmd = SET;
		        }
		        else {
		        	cmd = SQL;
		        }
		        main = mergeCommand(main, cmd);	        
	    	} while(main != SQL && (idx=skipWhiteSpace(sql, jumpTo(sql, idx, ';')+1)) < sql.length());
		}
        return main;
    }
	
	
	static int skipWhiteSpace(String s, int idx) {
		while(idx < s.length() && isWhitespace(s.charAt(idx))) {
			idx++;
		}
		return idx;
	}
	
	static int jumpTo(String s, int idx, char to) {
		var len =  s.length();
		var inQuotes = false;
		while(idx < len) {
			char c = s.charAt(idx);
			if (inQuotes) {
	            if (c == '\\' && idx + 1 < len) {
	                idx += 2;
	            }
	            if (c == '\'') {
	                if (idx + 1 < len && s.charAt(idx + 1) == '\'') { // double quotes
	                    idx ++; 
	                }
	                else {
	                	inQuotes = false; 
	                }
	            }
	        }
			else if (c == '\'') {
	            inQuotes = true;
	        }
			else if(c == '-' && idx+1 < len && s.charAt(idx+1) == '-') {
				while(idx < len && s.charAt(idx) != '\n') {
					idx++;
				}
				continue;
			}
			else if(c == to) {
				break;
			}
			idx++;
		}
		return idx;
	}
	
	static DatabaseCommand mergeCommand(DatabaseCommand main, DatabaseCommand cmd) {
		if(main == cmd || isNull(cmd)) {
			return main;
		}
		return isNull(main) ? cmd : SQL;
	}
}