package org.usf.traceapi.core;

import static java.lang.System.lineSeparator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.usf.traceapi.core.SqlCommand.mainCommand;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;

//https://www.guru99.com/sql-commands-dbms-query.html

class SqlCommandTest {
	
	private static final String whiteSpace = 
			lineSeparator() + " \t";
	
	@ParameterizedTest
	@CsvSource({
		"CREATE,'CREATE database university;'",
		"CREATE,'CREATE table students;'",
		"CREATE,'CREATE view for_students;'",
		"SELECT,'SELECT FirstName  FROM Student  WHERE RollNo > 15;'",
		"INSERT,'INSERT INTO TABLE_NAME(col1, col2, col3,.... col N) VALUES (value1, value2, value3, .... valueN);'",
		"UPDATE,'UPDATE students SET FirstName = ''Jhon'', LastName= ''Wick'' WHERE StudID = 3'",
		"DELETE,'DELETE FROM Students WHERE RollNo =25;",
		"SQL,'DELETE FROM Students WHERE RollNo =25;SELECT FirstName  FROM Student  WHERE RollNo > 15;",
		"GRANT,'GRANT SELECT ON Users TO''Tom''@''localhost;'",
		"REVOKE,'REVOKE SELECT, UPDATE ON student FROM BCA, MCA;'",
		"SELECT,'WITH avg_total_salary AS (SELECT AVG(salary) AS moy FROM employees) SELECT id, first_name, last_name,salary - moy  AS diff FROM employees, avg_total_salary;'"
	})
	void testMainCommand(SqlCommand cmd, String sql) {
		assertEquals(cmd, mainCommand(sql));
		assertEquals(cmd, mainCommand(sql.toLowerCase()));
		assertEquals(cmd, mainCommand(indent(sql)));
	}
	
	@ParameterizedTest
	@NullSource
	void testMainCommand_null(String sql) {
		assertThrows(NullPointerException.class, ()-> mainCommand(sql));
	}

	static String indent(String s) {
		return whiteSpace + 
				s.replaceAll("(WHERE|FROM|SET|\\()", lineSeparator()+"$1")
				.replaceAll("(;)", "$1"+lineSeparator()) +
				whiteSpace + ";";
	}
}
