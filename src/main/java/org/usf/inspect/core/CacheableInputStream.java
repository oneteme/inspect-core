package org.usf.inspect.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 * @author u$f
 *
 */
public final class CacheableInputStream extends InputStream {
	
	static final OutputStream NO_OUT = new OutputStream() {
		@Override
		public void write(int b) throws IOException {/* do nothing */}
	};
	
	private final InputStream in;
	private final OutputStream out;
	private int length;
 
	public CacheableInputStream(InputStream in, boolean cache) {
		this.in = in;
		this.out = cache ? new ByteArrayOutputStream() : NO_OUT;
	}
	
	//all read, trasfertTo & skip method must call super => this.read()
	
	@Override
	public int read() throws IOException {
		var b = in.read();
		if(b > -1) {
			out.write(b);
			++length;
		}
		return b;
	}
	
	@Override
	public int available() throws IOException {
		return in.available();
	}

	@Override
	public boolean markSupported() {
		return in.markSupported();
	}
	
	@Override
	public synchronized void mark(int readlimit) {
		in.mark(readlimit);
	}
	
	@Override
	public synchronized void reset() throws IOException {
		in.reset();
	}
	
	@Override
	public void close() throws IOException {
		try {
			out.close();
		}
		finally {
			in.close();
		}
	}
	
	public int getDataLength(){
		return length;
	}
	
	public byte[] getData() {
		return out instanceof ByteArrayOutputStream bos ? bos.toByteArray() : null;
	}
}
