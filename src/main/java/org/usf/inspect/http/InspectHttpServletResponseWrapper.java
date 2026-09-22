package org.usf.inspect.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicLong;

import org.usf.inspect.http.TransferPayload.StreamExchangeListener;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f 
 *
 */
public final class InspectHttpServletResponseWrapper extends HttpServletResponseWrapper {

	@Getter
	private final StreamExchangeListener listener;

	private ServletOutputStream outputStream;
	private PrintWriter writer;

    public InspectHttpServletResponseWrapper(HttpServletResponse response, StreamExchangeListener listener) {
        super(response);
        this.listener = listener;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (isNull(outputStream)) {
            this.outputStream = nonNull(listener) 
            		? new InspectServletOutputStream(super.getOutputStream(), listener) 
            		: outputStream;
        }
        return this.outputStream;
    }
    
    @Override
    public PrintWriter getWriter() throws IOException {
        if (isNull(writer)) {
        	var enc = getCharacterEncoding();
            var chr = nonNull(enc) ? Charset.forName(enc) : UTF_8;
            this.writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), chr));
        }
        return writer;
    }
    
    public long getWrittenBytes(){
    	return outputStream instanceof InspectServletOutputStream sos  ? sos.size.get() : -1;
    }

    @RequiredArgsConstructor
    private static final class InspectServletOutputStream extends ServletOutputStream {
        
    	private final ServletOutputStream delegate;
    	private final StreamExchangeListener listener;
    	private final AtomicLong size = new AtomicLong(-1);
    	
        @Override
        public void write(int b) throws IOException {
        	notifyStreamStarted();
            delegate.write(b);
            size.incrementAndGet();
        }

        @Override
        public void write(byte[] b) throws IOException {
            write(b, 0, b.length); //see OutputStream.write(byte[] b)
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            notifyStreamStarted();
            delegate.write(b, off, len);
            size.addAndGet(len);
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
            notifyStreamStarted();
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
        	try {
        		delegate.close();
        	}
        	finally {
        		if(size.get() > -1) { //not started
                	listener.onTransmissionEnd();
        		}
			}
        }
        
        private void notifyStreamStarted() {
            if(size.compareAndSet(-1, 0)) {
            	listener.onTransmissionStart();
            }
        }
    }
}