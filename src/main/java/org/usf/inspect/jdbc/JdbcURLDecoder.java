package org.usf.inspect.jdbc;

import static java.util.Objects.isNull;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JdbcURLDecoder {
	
	private static final String[] UNKNOWN = new String[] {null, null, null, null};
	
	private static final Pattern STP1 = compile("^jdbc:(\\w+):", CASE_INSENSITIVE);
	private static final Pattern STP2 = compile("^//([\\w-\\.]+)(:\\d+)?[,;/]?");
	private static final Pattern STP3 = compile("^(\\w+)([,;\\?].+)?$"); //mysql|postgresql|db2|mariadb
	private static final Pattern STP4 = compile("^.*database(?:Name)?=(\\w+)", CASE_INSENSITIVE); //teradata|sqlserver
	private static final Pattern STP5 = compile("^(?:file|mem):([\\w-\\.\\/]+)", CASE_INSENSITIVE);//H2 mem|file

	public static String[] decode(String url) {
		var m = STP1.matcher(url);
		if(m.find()) {
			List<String> arr = new ArrayList<>(3);
			var db = m.group(1).toLowerCase();
			arr.add(db);
			var m2 = STP2.matcher(url).region(m.end(), url.length());
			if(m2.find()) {
				arr.add(m2.group(1));
				var port = m2.group(2);
				arr.add(isNull(port) ? null : port.substring(1));
				if(m2.end() < url.length()) {
					var m3 = STP3.matcher(url).region(m2.end(), url.length());
					if(m3.find()) {
						arr.add(m3.group(1));
					}
					else {
						m3 = STP4.matcher(url).region(m2.end(), url.length());
						arr.add(m3.find() ? m3.group(1) : null);
					}
				}
				else {
					arr.add(null); //no base
				}
			}
			else { //h2
				arr.add(null); //no host
				arr.add(null); //no port
				m2 = STP5.matcher(url).region(m.end(), url.length());
				arr.add(m2.find() ? m2.group(1) : null);
			}
			return arr.toArray(String[]::new);
		}
		return UNKNOWN; //avoid null pointer
	}

}
