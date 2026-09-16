package org.usf.inspect.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Clock.systemUTC;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;

import org.usf.inspect.http.TransferPayload.StreamPayload;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.RequiredArgsConstructor;

public final class InspectResponseWrapper extends HttpServletResponseWrapper {

	private final StreamPayload payload;

	private ServletOutputStream outputStream;
	private PrintWriter writer;

    public InspectResponseWrapper(HttpServletResponse response, StreamPayload payload) {
        super(response);
        this.payload = payload;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (this.outputStream == null) {
            this.outputStream = new InspectOutputStream(super.getOutputStream(), payload);
        }
        return this.outputStream;
    }
    
    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
        	var enc = getCharacterEncoding();
            var chr = nonNull(enc) ? Charset.forName(enc) : UTF_8;
            this.writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), chr));
        }
        return writer;
    }

    @RequiredArgsConstructor
    private static final class InspectOutputStream extends ServletOutputStream {
        
    	private final ServletOutputStream delegate;
    	private final StreamPayload payload;
    	
        @Override
        public void write(int b) throws IOException {
        	ensureStart();
            delegate.write(b);
            payload.getSize().incrementAndGet();
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            ensureStart();
            delegate.write(b, off, len);
            payload.getSize().addAndGet(len);
        }
        
        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            delegate.setWriteListener(writeListener);
        }

        @Override
        public void flush() throws IOException {
            ensureStart();
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            ensureStart();
        	try {
        		delegate.close();
        	}
        	finally {
        		if(isNull(payload.getEnd())) {
                    payload.setEnd(systemUTC().instant());
        		}
			}
        }
        
        private void ensureStart() {
            if (isNull(payload.getStart())) {
            	payload.setStart(systemUTC().instant());
            }
        }
    }
}