package org.usf.inspect.core;

import static java.lang.Byte.MAX_VALUE;

import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
@Getter
public enum TraceType {
	
	MAIN_SES(1),
	HTTP_SES(2),
	HTTP_REQ(10),
	JDBC_REQ(11),
	SMTP_REQ(12),
	LDAP_REQ(13),
	FTP_REQ(14),
	LCL_REQ(99);
	
	private final byte value;
	
	TraceType(int value) {
		if(value > MAX_VALUE) {
			throw new ClassCastException("byte=" + value);
		}
		this.value = (byte) value;
	}
}