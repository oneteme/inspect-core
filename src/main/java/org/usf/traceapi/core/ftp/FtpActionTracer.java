package org.usf.traceapi.core.ftp;

import static java.time.Instant.now;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.log;

import java.sql.SQLException;
import java.time.Instant;
import java.util.function.Supplier;

import org.usf.traceapi.core.JDBCAction;
import org.usf.traceapi.core.JDBCActionTracer;
import org.usf.traceapi.core.JDBCActionTracer.DatabaseActionConsumer;
import org.usf.traceapi.core.JDBCActionTracer.SQLSupplier;

public class FtpActionTracer {

	private <T> T trace(JDBCAction action, Supplier<Instant> startSupp, SQLSupplier<T> sqlSupp, DatabaseActionConsumer cons) throws SQLException {
		log.trace("executing {} action..", action);
		SQLException ex = null;
		var beg = startSupp.get();
		try {
			return sqlSupp.get();
		}
		catch(SQLException e) {
			ex  = e;
			throw e;
		}
		finally {
			var fin = now();
			cons.accept(action, beg, fin, mainCauseException(ex));
		}
	}
}
