package org.usf.inspect.jdbc;

import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.usf.inspect.core.DatabaseCommand.extractCommand;

import java.util.regex.Pattern;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.platform.commons.annotation.Testable;
import org.usf.inspect.core.DatabaseCommand;

//https://www.guru99.com/sql-commands-dbms-query.html

@Testable
class SqlCommandTest {
	
	private static final String WHITESPACE = " \t " + lineSeparator(); 
	private static final String COMMENT = "-- DUMMY COMMENT )'(" + lineSeparator();
	private static final String[] keywords = { "CREATE", "DROP", "ALTER", "TRUNCATE", "GRANT", "REVOKE", "INSERT", "UPDATE", "WITH", "MERGE", "SELECT", "FROM", "AS" };
	private static final Pattern pattern = compile("\\b(" + join("|", keywords) + ")\\b\\s*", CASE_INSENSITIVE);
	private static final String replacement = "$1 " + COMMENT + "\s";
	
	@ParameterizedTest
	@CsvSource({
		// =========================
		// BASIC DDL / DML
		// =========================
		"CREATE,'CREATE DATABASE university;'",
		"CREATE,'CREATE TABLE students;'",
		"DROP,'DROP DATABASE university;'",
		"DROP,'DROP TABLE student;'",
		"ALTER,'ALTER TABLE student ADD subject VARCHAR;'",
		"TRUNCATE,'TRUNCATE TABLE students;'",
		"GRANT,'GRANT SELECT ON Users TO''Tom''@''localhost;'",
		"REVOKE,'REVOKE SELECT, UPDATE ON student FROM BCA, MCA;'",
		"INSERT,'INSERT INTO students (RollNo, FIrstName, LastName) VALUES (''60'', ''Tom'', ''value (fake parenthèse)'')'",
		"UPDATE,'UPDATE students SET FirstName = ''Jhon'', LastName= ''Wick'' WHERE StudID = 3'",
		"DELETE,'DELETE FROM Students WHERE RollNo =25;",
		"SELECT,'SELECT FirstName  FROM Student  WHERE RollNo > 15;'",
		
		// =========================
		// SIMPLE DML
		// =========================
		"INSERT,'INSERT INTO students (id, name) VALUES (1, ''Tom'');'",
		"UPDATE,'UPDATE students SET name = ''John'' WHERE id = 3;'",
		"DELETE,'DELETE FROM students WHERE id = 25;'",

		// =========================
		// SELECT VARIANTS
		// =========================
		"SELECT,'SELECT * FROM users;'",
		"SELECT,'SELECT id, name FROM students WHERE id > 10;'",
		"SELECT,'SELECT COUNT(*) FROM orders;'",


		// =========================
		// INSERT WITH SELECT
		// =========================
		"INSERT,'INSERT INTO archive_users SELECT * FROM users;'",
		"INSERT,'INSERT INTO result_table (id, total) SELECT id, SUM(price) FROM orders GROUP BY id;'",


		// =========================
		// DELETE WITH SUBQUERY
		// =========================
		"DELETE,'DELETE FROM users WHERE id IN (SELECT id FROM banned_users);'",

		// =========================
		// UPDATE WITH SUBQUERY
		// =========================

		"UPDATE,'UPDATE users SET score = (SELECT AVG(score) FROM users);'",

		// =========================
		// NESTED SELECTS
		// =========================
		"SELECT,'SELECT * FROM (SELECT id FROM users) u;'",
		"SELECT,'SELECT * FROM (SELECT id FROM (SELECT id FROM users) x) y;'",

		// =========================
		// SINGLE CTE
		// =========================

		"SELECT,'WITH cte AS (SELECT id FROM users) SELECT * FROM cte;'",

		// =========================
		// MULTIPLE CTEs
		// =========================

		"SELECT,'WITH cte1 AS (SELECT id FROM users), cte2 AS (SELECT id FROM orders) SELECT * FROM cte1;'",
		"SELECT,'WITH a AS (SELECT 1), b AS (SELECT 2), c AS (SELECT 3) SELECT * FROM a;'",

		// =========================
		// COMPLEX CTE WITH JOIN
		// =========================

		"SELECT,'WITH sales AS (SELECT id, amount FROM orders), avg_sales AS (SELECT AVG(amount) AS avg FROM sales) SELECT id, amount FROM sales, avg_sales WHERE amount > avg;'",


		// =========================
		// CTE WITH SUBQUERIES
		// =========================

		"SELECT,'WITH cte AS (SELECT id FROM (SELECT id FROM users) x) SELECT * FROM cte;'",
		"SELECT,'WITH avg_salary AS (SELECT AVG(salary) AS moy FROM employees) SELECT id, first_name, last_name,salary - moy  AS diff FROM employees, avg_salary;'",
		"SELECT,'WITH cte_sales AS(SELECT EmployeeID, COUNT(OrderID) as Orders, ShipperID FROM Orders GROUP BY EmployeeID, ShipperID), shipper_cte AS (SELECT * FROM cte_sales WHERE ShipperID=2 or ShipperID=3) SELECT ShipperID, AVG(Orders) average_order_per_employee FROM shipper_cte GROUP BY ShipperID;'",


		// =========================
		// MULTI-STATEMENT INPUT
		// =========================

		"SQL,'SELECT * FROM users; DELETE FROM users WHERE id = 1;'",
		"SQL,'INSERT INTO a VALUES (1); INSERT INTO b VALUES (2); UPDATE c SET x = 1;'",
		"CREATE,'CREATE TABLE students;CREATE VIEW for_students as select *;'",
		"SQL,'DELETE FROM Students WHERE RollNo = 25; SELECT FirstName FROM Student  WHERE RollNo > 15;",
		
		// =========================
		// CHAOTIC FORMATTING
		// =========================
		"SELECT,'   SELECT   *   FROM   users   ;   '",
		"SELECT,'WITH cte AS ( SELECT id FROM users ) SELECT * FROM cte ;'",
		"SELECT,'WITH a AS (\nSELECT 1\n), b AS (\nSELECT 2\n)\nSELECT * FROM a;'",

		// =========================
		// EDGE CASES
		// =========================
		"SELECT,'SELECT '';'' AS test FROM dual;'",
		"SELECT,'SELECT * FROM logs WHERE message LIKE ''%;%'';'",
		"SQL,'WITH a AS (SELECT 1) SELECT * from t; DROP TABLE users; SELECT 2;'"
	})
	void testMainCommand(DatabaseCommand cmd, String sql) {
		assertEquals(cmd, extractCommand(sql));
		assertEquals(cmd, extractCommand(sql.toLowerCase()));
		assertEquals(cmd, extractCommand(addComment(sql)));
		assertEquals(cmd, extractCommand(indent(sql)));
	}
	@ParameterizedTest
	@CsvSource({
		"'CREATE SCHEMA university;'",
		"'CREATE TABLE students;'",
		"'CREATE VIEW for_students as select *;'",
		"'DROP OBJECT_TYPE object_name;'",
		"'DROP SCHEMA university;",
		"'DROP TABLE student;",
		"'ALTER TABLE student ADD subject VARCHAR;'",
		"'TRUNCATE TABLE students;'",
		"'GRANT SELECT ON Users TO''Tom''@''localhost;'",
		"'REVOKE SELECT, UPDATE ON student FROM BCA, MCA;'",
		"'INSERT INTO students (RollNo, FIrstName, LastName) VALUES (''60'', ''Tom'', ''Erichsen'')'",
		"'UPDATE students SET FirstName = ''Jhon'', LastName= ''Wick'' WHERE StudID = 3'",
		"'DELETE FROM Students WHERE RollNo =25;",
		"'SELECT FirstName  FROM Student  WHERE RollNo > 15;'",
		"'WITH avg_salary AS (SELECT AVG(salary) AS moy FROM employees) SELECT id, first_name, last_name,salary - moy  AS diff FROM employees, avg_salary;'",
		"'WITH cte_sales AS(SELECT EmployeeID, COUNT(OrderID) as Orders, ShipperID FROM Orders GROUP BY EmployeeID, ShipperID), shipper_cte AS (SELECT * FROM cte_sales WHERE ShipperID=2 or ShipperID=3) SELECT ShipperID, AVG(Orders) average_order_per_employee FROM shipper_cte GROUP BY ShipperID;'",
		"'DELETE FROM Students WHERE RollNo = 25; SELECT FirstName FROM Student  WHERE RollNo > 15;",
		"'CREATE TABLE students;CREATE VIEW for_students as select *;'",
	})
	void testAddComment( String sql) {
//		assertEquals(sql, addComment(sql));
	}

	@ParameterizedTest
	@NullSource
	@CsvSource({
		"DUMMY SQL COMMAND",
		"'WITH avg_salary AS query SELECT id, first_name, last_name,salary - moy  AS diff FROM employees, avg_salary;'",
		"'WITH avg_salary AS (SELECT AVG salary) AS moy FROM employees) SELECT id, first_name, last_name,salary - moy  AS diff FROM employees, avg_salary;'",
	})
	void testMainCommand_unknown(String sql) {
		assertNull(extractCommand(sql));
	}
	
	static String indent(String s) {
		return WHITESPACE + 
				s.replaceAll("\s+(WHERE|FROM|SET)", lineSeparator()+"$1")
				.replaceAll("(\\)|;)\s*", "$1"+lineSeparator()) +
				WHITESPACE ;
	}
	
	static String addComment(String s) {
		return pattern.matcher(s).replaceAll(replacement);
	}
}
