package org.usf.inspect.jdbc;

import static java.util.Locale.ROOT;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.With;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JdbcURLDecoder {
	
	private static final JdbcUrlParts UNKNOWN_PARTS = new JdbcUrlParts(null, null, null, null);

	private static final String JDBC_PREFIX = "jdbc:";
	private static final String ORACLE_VENDOR = "oracle";
	private static final String H2_VENDOR = "h2";

	public static JdbcUrlParts decode(String url) {
		if(url == null || !startsWithIgnoreCase(url, JDBC_PREFIX)) {
			return UNKNOWN_PARTS;
		}
		var dbTypeEnd = url.indexOf(':', JDBC_PREFIX.length());
		if(dbTypeEnd < 0) {
			return UNKNOWN_PARTS;
		}
		var vndr = url.substring(JDBC_PREFIX.length(), dbTypeEnd).toLowerCase(ROOT);
		var tail = url.substring(dbTypeEnd + 1);
		if(H2_VENDOR.equals(vndr)) {
			return parseH2(tail);
		}
		if(ORACLE_VENDOR.equals(vndr)) {
			return parseOracle(tail);
		}
		var prt = parseAuthorityAndPath(vndr, tail);
		if(isNull(prt.database)) {
			prt = prt.withDatabase(switch(vndr) {
				case "sqlserver" -> valueOfKey(tail, "databaseName=");
				case "teradata"  -> valueOfKey(tail, "database=");
				default -> null;
			});
		}
		return prt;
	}

	static JdbcUrlParts parseH2(String tail) {
		if(startsWithIgnoreCase(tail, "mem:")) {
			return new JdbcUrlParts(H2_VENDOR, "mem", null, tail.substring(4));
		}
		else if(startsWithIgnoreCase(tail, "file:")) {
			var idx = tail.lastIndexOf("/", tail.length()-1);
			if(idx > -1 && idx+1 < tail.length()) {
				return new JdbcUrlParts(H2_VENDOR, tail.substring(0, idx), null, tail.substring(idx+1));
			}
		}
		return new JdbcUrlParts(H2_VENDOR, tail, null, null);
	}

	static JdbcUrlParts parseOracle(String tail) {
		var idx = tail.indexOf('@');
		if(idx < 0 || idx+1>= tail.length()) {
			return UNKNOWN_PARTS;
		}
		var descriptor = tail.substring(idx + 1);
		if(descriptor.startsWith("//")) {
			return parseAuthorityAndPath(ORACLE_VENDOR, descriptor);
		}
		var firstColon = descriptor.indexOf(':');
		if(firstColon > -1) {
			var secondColon = descriptor.indexOf(':', firstColon+1);
			if(secondColon > -1) {
				return new JdbcUrlParts(ORACLE_VENDOR, 
						descriptor.substring(0, firstColon), 
						descriptor.substring(firstColon+1, secondColon),
						tokenUntil(descriptor, secondColon+1, '/', '?', ';', ','));
			}
			return new JdbcUrlParts(ORACLE_VENDOR, 
					descriptor.substring(0, firstColon), 
					null,
					tokenUntil(descriptor, firstColon+1, '/', '?', ';', ','));
		}
		return UNKNOWN_PARTS;
	}

	static JdbcUrlParts parseAuthorityAndPath(String vendor, String tail) {
		var offset = 0;
		if(startsWithIgnoreCase(tail, "http://")) {
			offset = 7;
		}
		else if(startsWithIgnoreCase(tail, "https://")) {
			offset = 8;
		}
		else if(tail.startsWith("//")) {
			offset = 2;
		}
		if(offset == 0 || offset >= tail.length()) {
			return UNKNOWN_PARTS;
		}
		var authorityEnd = indexOfAny(tail, offset, '/', ';', '?');
		if(authorityEnd < 0) {
			authorityEnd = tail.length();
		}
		if(authorityEnd <= offset) {
			var host = tokenUntil(tail, offset, '?', ';', ',');
			return new JdbcUrlParts(vendor.toLowerCase(ROOT), host, null, null);
		}
		var prt = parseHostPort(vendor.toLowerCase(ROOT), tail.substring(offset, authorityEnd));
		if(authorityEnd < tail.length() && tail.charAt(authorityEnd) == '/') {
			var dbName = tokenUntil(tail, authorityEnd+1, '?', ';', ',');
			if(nonNull(dbName) && !dbName.isEmpty() && !dbName.contains("=")) { //avoid key=value pairs
				prt = prt.withDatabase(dbName);
			}
		}
		return prt;
	}

	private static JdbcUrlParts parseHostPort(String vendor, String authority) {
		var firstNodeEnd = authority.indexOf(','); //multiple hosts are supported by some vendors (e.g. MySQL, PostgreSQL, Cassandra, etc.)
		var firstNode = firstNodeEnd > -1 ? authority.substring(0, firstNodeEnd) : authority;
		if(firstNode.startsWith("[")) { //IPv6 address
			var closingBracket = firstNode.indexOf(']');
			if(closingBracket > 0) {
				var host = firstNode.substring(1, closingBracket);
				String port = null;
				if(closingBracket+2 < firstNode.length() && firstNode.charAt(closingBracket+1) == ':') {
					port = firstNode.substring(closingBracket + 2);
				}
				return new JdbcUrlParts(vendor, host, port, null);
			}
		}
		var colon = firstNode.indexOf(':');
		if(colon > -1) {
			var host = firstNode.substring(0, colon);
			String port = null;
			if(colon+1 < firstNode.length()) {
				port = firstNode.substring(colon + 1);
			}
			return new JdbcUrlParts(vendor, host, port, null);
		}
		return new JdbcUrlParts(vendor, firstNode, null, null);
	}

	static String valueOfKey(String text, String databaseKey) { //key is case insensitive + separator
		var from = text.toLowerCase(ROOT).indexOf(databaseKey.toLowerCase(ROOT));
		return from < 0 ? null : tokenUntil(text, from + databaseKey.length(), ';', ',', '?', '&');
	}

	static String tokenUntil(String source, int start, char... separators) {
		if(start < source.length()) {
			var end = source.length();
			for(var i = start; i<end; i++) {
				var ch = source.charAt(i);
				for (var separator : separators) {
					if(ch == separator) {
						return source.substring(start, i);
					}
				}
			}
			return source.substring(start, end);
		}
		return null;
	}

	static int indexOfAny(String source, int start, char... separators) {
		var len = source.length();
		for(var i = start; i < len; i++) {
			var c = source.charAt(i);
			for (var spr : separators) {
				if(c == spr) {
					return i;
				}
			}
		}
		return -1;
	}

	static boolean startsWithIgnoreCase(String text, String prefix) {
		return text.regionMatches(true, 0, prefix, 0, prefix.length());
	}
	
	@Getter
	@With
	@RequiredArgsConstructor
	static class JdbcUrlParts {
		
		final String vendor;
		final String host;
		final String port;
		final String database;
	}
}
