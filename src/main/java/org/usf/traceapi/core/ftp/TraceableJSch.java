package org.usf.traceapi.core.ftp;

import static java.time.Instant.now;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
//import static org.usf.traceapi.core.Helper.stackTraceElement;
//import static org.usf.traceapi.core.Helper.threadName;

import org.usf.traceapi.core.FtpRequest;
import org.usf.traceapi.core.Helper;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.TraceableSession;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
public class TraceableJSch extends JSch {
	
	@Override
	public TraceableSession getSession(String username, String host, int port) throws JSchException {
		if(host==null){
			throw new JSchException("host must not be null.");
	    }
		JSchException ex = null;
		var req = new FtpRequest();
		var beg = now();
		try {
			return new TraceableSession(this, username, host, port, req);
		}
		catch(JSchException e) {
			ex  = e;
			throw e;
		}
		finally {
			var fin = now();
			req.setStart(beg);
			req.setEnd(fin);
//			req.setThreadName(threadName());
//			req.setException(mainCauseException(ex));
//			stackTraceElement().ifPresent(st->{
//				req.setName(st.getMethodName());
//				req.setLocation(st.getClassName());
//			});
		}
	}

}
