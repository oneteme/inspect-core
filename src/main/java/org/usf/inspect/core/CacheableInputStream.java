package org.usf.inspect.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
public final class CacheableInputStream extends InputStream {
	
	static final OutputStream NO_OUT = new OutputStream() { //nullOutputStream may throws Exception
		@Override
		public void write(int b) throws IOException {/* do nothing */}
	};
	
	private static final int MAX_SIZE = 10_000; //10k

	@Delegate
	private final InputStream in;
	private final OutputStream out;
	private long length;
 
	public CacheableInputStream(InputStream in, boolean cache) {
		this.in = in;
		this.out = cache ? new ByteArrayOutputStream() : NO_OUT;
	}
	
	@Override
	public int read() throws IOException {
		var b = in.read();
		if(b > -1) {
			var v = assertNextLength(1);
			if(v > 0) {
				out.write(b);
			}
			++length;
		}
		return b;
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		var n = in.read(b);
		if(n > -1) {
			var v = assertNextLength(n);
			if(v > 0) {
				out.write(b, 0, v);
			}
			length += n;
		}
		return n;
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		var n = in.read(b, off, len);
		if(n > -1) {
			var v = assertNextLength(len - off);
			if(v > 0) {
				out.write(b, 0, v);
			}
			length += n;
		}
		return n;
	}
	
	@Override
	public byte[] readAllBytes() throws IOException {
		var arr = in.readAllBytes();
		if(arr.length > 0) {
			var v = assertNextLength(arr.length);
			if(v > 0) {
				out.write(arr, 0, v);
			}
			length += arr.length;
		}
		return arr;
	}
	
	@Override
	public byte[] readNBytes(int len) throws IOException {
		var arr = in.readNBytes(len);
		if(arr.length > 0) {
			var v = assertNextLength(arr.length);
			if(v > 0) {
				out.write(arr, 0, v);
			}
			length += arr.length;
		}
		return arr;
	}
	
	@Override
	public int readNBytes(byte[] b, int off, int len) throws IOException {
		var n = in.readNBytes(b, off, len);
		if(n > 0) {
			var v = assertNextLength(n);
			if(v > 0) {
				out.write(b, off, v);
			}
			length += n;
		}
		return n;
	}
	
	int assertNextLength(int n) {
		if(length >= MAX_SIZE) { // avoid overflow
			return 0;
		}
		var size = length + n;
		if(size > MAX_SIZE) {
			return MAX_SIZE - (int)length; // return remaining size
		}
		return n;
	}
	
	@Override
	public void close() throws IOException {
		try {
			if(out instanceof ByteArrayOutputStream) {
				out.close();
			}
		}
		finally {
			in.close();
		}
	}
	
	public long getDataLength(){
		return length;
	}
	
	public byte[] getData() {
		return out instanceof ByteArrayOutputStream bos ? bos.toByteArray() : null;
	}
}
