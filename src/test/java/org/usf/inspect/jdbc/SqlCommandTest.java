package org.usf.inspect.jdbc;

import static java.lang.System.lineSeparator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.usf.inspect.core.DatabaseCommand.parseCommand;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.usf.inspect.core.DatabaseCommand;

//https://www.guru99.com/sql-commands-dbms-query.html

class SqlCommandTest {
	
	private static final String WHITESPACE = " \t " + lineSeparator();
	
	@ParameterizedTest
	@CsvSource({
		"CREATE,'CREATE DATABASE university;'",
		"CREATE,'CREATE TABLE students;'",
		"CREATE,'CREATE VIEW for_students;'",
		"DROP,'DROP OBJECT_TYPE object_name;'",
		"DROP,'DROP DATABASE university;",
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
		"SQL,'CREATE TABLE students;CREATE VIEW for_students;'",
	})
	void testMainCommand(DatabaseCommand cmd, String sql) {
		assertEquals(cmd, parseCommand(sql));
		assertEquals(cmd, parseCommand(sql.toLowerCase()));
		assertEquals(cmd, parseCommand(indent(sql)));
	}

	@ParameterizedTest
	@CsvSource({
		"DUMMY SQL COMMAND",
		"'WITH avg_salary AS query SELECT id, first_name, last_name,salary - moy  AS diff FROM employees, avg_salary;'",
		"'WITH avg_salary AS (SELECT AVG salary) AS moy FROM employees) SELECT id, first_name, last_name,salary - moy  AS diff FROM employees, avg_salary;'",
	})
	void testMainCommand_unknown(String sql) {
		assertEquals(null, parseCommand(sql));
	}

	@ParameterizedTest
	@NullSource
	void testMainCommand_null(String sql) {
		assertThrows(NullPointerException.class, ()-> parseCommand(sql));
	}
	
	static String indent(String s) {
		return WHITESPACE + 
				s.replaceAll("\s+(WHERE|FROM|SET)", lineSeparator()+"$1")
				.replaceAll("(\\)|;)\s*", "$1"+lineSeparator()) +
				WHITESPACE + ";";
	}
}
