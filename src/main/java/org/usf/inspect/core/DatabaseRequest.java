package org.usf.inspect.core;

import static org.usf.inspect.core.Helper.prettyURLFormat;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class DatabaseRequest extends AbstractRequest<DatabaseRequestStage> {

	private String scheme;
	private String host; //IP, domaine
	private int port; //-1 otherwise
	private String name; //nullable
	private String schema;
	private String driverVersion;
	private String productName;
	private String productVersion;
	//java-collector

	@Override
	public Metric copy() {
		var req = new DatabaseRequest();
		req.setId(getId());
		req.setStart(getStart());
		req.setEnd(getEnd());
		req.setUser(getUser());
		req.setThreadName(getThreadName());
		req.setSessionId(getSessionId());
		req.setScheme(scheme);
		req.setHost(host);
		req.setPort(port);
		req.setName(name);
		req.setSchema(schema);
		req.setDriverVersion(driverVersion);
		req.setProductName(productName);
		req.setProductVersion(productVersion);
		return req;
	}
	
	@Override
	protected DatabaseRequestStage createStage() {
		return new DatabaseRequestStage();
	}
	
	@Override
	public String prettyFormat() {
		return '['+productName+']' 
				+ prettyURLFormat(getUser(), "jdbc", host, port, name);
	}
}
