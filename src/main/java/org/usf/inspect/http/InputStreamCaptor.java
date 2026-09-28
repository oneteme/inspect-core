package org.usf.inspect.http;

import static java.io.OutputStream.nullOutputStream;
import static java.lang.Math.min;

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
public final class InputStreamCaptor extends InputStream implements StreamCaptor {

	private static final int MAX_BYTES_TO_CAPTURE = 4096; //4ko
	
	static final OutputStream NULL_OUT = nullOutputStream();

	@Delegate
	private final InputStream in;
	private final OutputStream out;
	private int length;
 
	public InputStreamCaptor(InputStream in, boolean cache) {
		this.in = in;
		this.out = cache ? new ByteArrayOutputStream() : NULL_OUT;
	}
	
	@Override
	public int read() throws IOException {
		var b = in.read();
		cacheByte(b);
		return b;
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		var n = in.read(b);
		cacheBytes(b, 0, n);
		return n;
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		var n = in.read(b, off, len);
		cacheBytes(b, off, n);
		return n;
	}
	
	@Override
	public int readNBytes(byte[] b, int off, int len) throws IOException {
		var n = in.readNBytes(b, off, len);
		cacheBytes(b, off, n);
		return n;
	}
	
	@Override
	public byte[] readAllBytes() throws IOException {
		var b = in.readAllBytes();
		cacheBytes(b, 0, b.length);
		return b;
	}
	
	@Override
	public byte[] readNBytes(int len) throws IOException {
		var b = in.readNBytes(len);
		cacheBytes(b, 0, b.length);
		return b;
	}
	
	void cacheBytes(byte[] b, int off, int len) throws IOException {
		if(len > 0) {
			var v = remainingCacheCapacity(len);
			if(v > 0) {
				out.write(b, off, v);
			}
			length += len;
		}
	}
	
	void cacheByte(int b) throws IOException {
		if(b > -1) {
			var v = remainingCacheCapacity(1);
			if(v > 0) {
				out.write(b);
			}
			++length;
		}
	}
	
	int remainingCacheCapacity(int n) {
		return length < MAX_BYTES_TO_CAPTURE ? min(n, MAX_BYTES_TO_CAPTURE-length) : 0; // return remaining size
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
	
	public long size(){
		return length;
	}
	
	public byte[] bytes() {
		return out instanceof ByteArrayOutputStream bos ? bos.toByteArray() : null;
	}
	
	@Override
	public Throwable throwable() {
		return null;
	}
}
