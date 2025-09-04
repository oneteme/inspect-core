package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;
/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class FtpRequest extends AbstractRequest {

	private String protocol; //FTP, FTPS
	private String host;
	private int port;  // -1 otherwise
	private String serverVersion;
	private String clientVersion;
	//v1.1
	private boolean failed;
	//ftp-collector

	@JsonCreator() public FtpRequest() { }

	FtpRequest(FtpRequest req) {
		super(req);
		this.protocol = req.protocol;
		this.host = req.host;
		this.port = req.port;
		this.serverVersion = req.serverVersion;
		this.clientVersion = req.clientVersion;
		this.failed = req.failed;
	}
	
	public FtpRequestStage createStage(FtpAction type, Instant start, Instant end, FtpCommand cmd, Throwable thrw, String... args) {
		runSynchronized(()->{ 
			if(nonNull(cmd)) {
				setCommand(merge(getCommand(), cmd.getType()));
			}
			if(nonNull(thrw)) {
				failed = true; 
			}
		});
		var stg = createStage(type, start, end, cmd, thrw, FtpRequestStage::new);
		stg.setArgs(args);
		return stg;
	}
	
	@Override
	public FtpRequest copy() {
		return new FtpRequest(this);
	}
	
	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withThread(getThreadName())
		.withAction(getCommand())
		.withUser(getUser())
		.withUrlAsTopic(protocol, host, port, null, null)
		.withPeriod(getStart(), getEnd())
		.format();
	}

	@Override
	public boolean equals(Object obj) {
		return CompletableMetric.areEquals(this, obj);
	}
	
	@Override
	public int hashCode() {
		return CompletableMetric.hashCodeOf(this);
	}
}
