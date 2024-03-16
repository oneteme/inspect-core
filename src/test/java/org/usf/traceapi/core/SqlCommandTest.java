package org.usf.traceapi.core;

import static java.lang.System.lineSeparator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.usf.traceapi.core.SqlCommand.mainCommand;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;

//https://www.guru99.com/sql-commands-dbms-query.html

class SqlCommandTest {
	
	private static final String whiteSpace = " \t" + lineSeparator();
	
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
		"SQL,'DELETE FROM Students WHERE RollNo =25;SELECT FirstName  FROM Student  WHERE RollNo > 15;",
	})
	void testMainCommand(SqlCommand cmd, String sql) {
		assertEquals(cmd, mainCommand(sql));
		assertEquals(cmd, mainCommand(sql.toLowerCase()));
		assertEquals(cmd, mainCommand(indent(sql)));
		System.out.println(indent(sql));
	}
	
	@ParameterizedTest
	@NullSource
	void testMainCommand_null(String sql) {
		assertThrows(NullPointerException.class, ()-> mainCommand(sql));
	}

	static String indent(String s) {
		return whiteSpace + 
				s.replaceAll("\s+(WHERE|FROM|SET)", lineSeparator()+"$1")
				.replaceAll("(\\)|;)\s*", "$1"+lineSeparator()) +
				whiteSpace + ";";
	}
}
