package org.usf.inspect.jdbc;

import static java.lang.System.lineSeparator;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.usf.inspect.core.DatabaseCommand.extractCommands;

import java.util.concurrent.ExecutorService;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.platform.commons.annotation.Testable;
import org.usf.inspect.core.DatabaseCommand;

import net.sf.jsqlparser.JSQLParserException;

//https://www.guru99.com/sql-commands-dbms-query.html

@Testable
class SqlCommandTest {
	
	private static final String WHITESPACE = " \t " + lineSeparator();
	private static final ExecutorService E = newSingleThreadExecutor();
	
	@ParameterizedTest
	@CsvSource({
		"CREATE,'CREATE SCHEMA university;'",
		"CREATE,'CREATE TABLE students;'",
		"CREATE,'CREATE VIEW for_students as select *;'",
		"DROP,'DROP OBJECT_TYPE object_name;'",
		"DROP,'DROP SCHEMA university;",
		"DROP,'DROP TABLE student;",
		"ALTER,'ALTER TABLE student ADD subject VARCHAR;'",
		"TRUNCATE,'TRUNCATE TABLE students;'",
		"GRANT,'GRANT SELECT ON Users TO''Tom''@''localhost;'",
		"REVOKE,'REVOKE SELECT, UPDATE ON student FROM BCA, MCA;'",
		"INSERT,'INSERT INTO students (RollNo, FIrstName, LastName) VALUES (''60'', ''Tom'', ''Erichsen'')'",
		"UPDATE,'UPDATE students SET FirstName = ''Jhon'', LastName= ''Wick'' WHERE StudID = 3'",
		"DELETE,'DELETE FROM Students WHERE RollNo =25;",
		"SELECT,'SELECT FirstName  FROM Student  WHERE RollNo > 15;'",
		"SELECT,'WITH avg_salary AS (SELECT AVG(salary) AS moy FROM employees) SELECT id, first_name, last_name,salary - moy  AS diff FROM employees, avg_salary;'",
		"SELECT,'WITH cte_sales AS(SELECT EmployeeID, COUNT(OrderID) as Orders, ShipperID FROM Orders GROUP BY EmployeeID, ShipperID), shipper_cte AS (SELECT * FROM cte_sales WHERE ShipperID=2 or ShipperID=3) SELECT ShipperID, AVG(Orders) average_order_per_employee FROM shipper_cte GROUP BY ShipperID;'",
		"SQL,'DELETE FROM Students WHERE RollNo = 25; SELECT FirstName FROM Student  WHERE RollNo > 15;",
		"CREATE,'CREATE TABLE students;CREATE VIEW for_students as select *;'",
	})
	void testMainCommand(DatabaseCommand cmd, String sql) throws JSQLParserException {
		assertEquals(cmd, extractCommands(sql));
		assertEquals(cmd, extractCommands(sql.toLowerCase()));
		assertEquals(cmd, extractCommands(indent(sql)));
	}

	@ParameterizedTest
	@NullSource
	@CsvSource({
		"DUMMY SQL COMMAND",
		"'WITH avg_salary AS query SELECT id, first_name, last_name,salary - moy  AS diff FROM employees, avg_salary;'",
		"'WITH avg_salary AS (SELECT AVG salary) AS moy FROM employees) SELECT id, first_name, last_name,salary - moy  AS diff FROM employees, avg_salary;'",
	})
	void testMainCommand_unknown(String sql) {
		assertNull(extractCommands(sql));
	}
	
	static String indent(String s) {
		return WHITESPACE + 
				s.replaceAll("\s+(WHERE|FROM|SET)", lineSeparator()+"$1")
				.replaceAll("(\\)|;)\s*", "$1"+lineSeparator()) +
				WHITESPACE + ";";
	}
}
