package org.usf.inspect.core;

import static java.util.Arrays.stream;
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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;

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

	public static DatabaseCommand extractCommands(String sql) {
        try {
        	System.out.println("SQL : " + sql);
            var statements = CCJSqlParserUtil.parseStatements(sql);
            return resolveCommand(statements);
        } catch (Exception e) {
            System.err.println("Erreur de parsing : " + e.getMessage());
        }
		return null;
    }
//	public static void extract(String sql) {
//		try {
//			// 1. Parser la requête
//			var statement = CCJSqlParserUtil.parse(sql);
//			
//			
//			// 2. Récupérer le type de commande (SELECT, INSERT, UPDATE, DELETE, etc.)
//			// getClass().getSimpleName() renvoie "Select", "Update", "Insert"...
////            String commandType = statement.getClass().getSimpleName().toUpperCase();
//			
//			// 3. Extraire toutes les tables (incluant les schémas si présents)
//			var tablesNamesFinder = new TablesNamesFinder();
//			List<String> tableList = tablesNamesFinder.getTableList(statement);
//			
//			// Affichage des résultats
//			System.out.println("Commande : " + resolveCommand(statement));
//			System.out.println("Tables impactées : " + tableList);
//			
//		} catch (Exception e) {
//			System.err.println("Erreur de parsing : " + e.getMessage());
//		}
//	}
	
	private static DatabaseCommand resolveCommand(Statement stmt) {
		System.out.println("Statement : " + stmt.getClass() + " \nname : " + stmt.getClass().getSimpleName());
		return switch (stmt) {

		case Select s -> SELECT;
		case Insert i -> INSERT;
		case Update u -> UPDATE;
		case Delete d -> DELETE;

		default -> {
			String name = stmt.getClass().getSimpleName().toUpperCase();
			DatabaseCommand result = stream(DatabaseCommand.values())
					.filter(cmd -> name.startsWith(cmd.name()))
					.findFirst().orElse(SQL);

			yield result;
		}
		};

	}
	private static DatabaseCommand resolveCommand(Statements stmts) {
		return stmts.size() > 1 ? SQL : resolveCommand(stmts.getFirst());
	}

    public static void main(String[] args) {
//    	System.out.println(extractCommands("CREATE DATABASE university;"));
    	System.out.println(extractCommands("CREATE TABLE students;"));
    	System.out.println(extractCommands("CREATE VIEW for_students AS SELECT * FROM students;")); // CREATE VIEW Needs AS ...subquery
//    	System.out.println(extractCommands("DROP OBJECT_TYPE object_name;"));
//    	System.out.println(extractCommands("DROP DATABASE university;"));
//    	System.out.println(extractCommands("DROP TABLE student;"));
//    	System.out.println(extractCommands("ALTER TABLE student ADD subject VARCHAR;"));
//    	System.out.println(extractCommands("TRUNCATE TABLE students;"));
//    	System.out.println(extractCommands("GRANT SELECT ON Users TO''Tom''@''localhost;"));
//    	System.out.println(extractCommands("REVOKE SELECT, UPDATE ON student FROM BCA, MCA;"));
//    	System.out.println(extractCommands("INSERT INTO students (RollNo, FIrstName, LastName) VALUES (''60'', ''Tom'', ''Erichsen'')"));
//    	System.out.println(extractCommands("UPDATE students SET FirstName = ''Jhon'', LastName= ''Wick'' WHERE StudID = 3"));
//    	System.out.println(extractCommands("DELETE FROM Students WHERE RollNo =25;"));
//    	System.out.println(extractCommands("SELECT FirstName  FROM Student  WHERE RollNo > 15;"));
//    	System.out.println(extractCommands("WITH avg_salary AS (SELECT AVG(salary) AS moy FROM employees) SELECT id, first_name, last_name,salary - moy  AS diff FROM employees, avg_salary;"));
//    	System.out.println(extractCommands("WITH cte_sales AS(SELECT EmployeeID, COUNT(OrderID) as Orders, ShipperID FROM Orders GROUP BY EmployeeID, ShipperID), shipper_cte AS (SELECT * FROM cte_sales WHERE ShipperID=2 or ShipperID=3) SELECT ShipperID, AVG(Orders) average_order_per_employee FROM shipper_cte GROUP BY ShipperID;"));
//    	System.out.println(extractCommands("DELETE FROM Students WHERE RollNo = 25; SELECT FirstName FROM Student  WHERE RollNo > 15;"));
    	System.out.println("Result : "+ extractCommands("CREATE TABLE students;CREATE VIEW for_students AS SELECT * FROM students;"));
//        extract("SELECT * FROM production.users u JOIN logs.access l ON u.id = l.user_id");
//        extract("INSERT INTO archive.history (id, data) VALUES (1, 'test')");
    }
}
