package org.usf.inspect.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lombok.Getter;

public final class InputStreamPipe extends InputStream {
	
	public static final OutputStream NO_CACHE = new OutputStream() {
		@Override
		public void write(int b) throws IOException {/* do nothing */}
	};
	
	private final InputStream in;
	private final OutputStream out;
	private int length;
	
	public InputStreamPipe(InputStream in, boolean cacheContent) {
		this.in = in;
		this.out = cacheContent ? new ByteArrayOutputStream() : NO_CACHE;
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
		in.close();
	}
	
	public int getDataLength(){
		return length;
	}
	
	public byte[] getData() {
		return out instanceof ByteArrayOutputStream bos ? bos.toByteArray() : null;
	}
}
