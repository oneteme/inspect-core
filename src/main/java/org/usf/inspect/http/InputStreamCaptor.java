package org.usf.inspect.http;

import static java.io.OutputStream.nullOutputStream;
import static java.lang.Math.min;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
public final class InputStreamCaptor extends InputStream implements StreamCaptor {

	private static final int MAX_BYTES_TO_CAPTURE = 4096; //4ko
	
	static final OutputStream NULL_OUT = nullOutputStream(); //never close it

	@Delegate
	private final InputStream in;
	private final OutputStream out;

	private final StreamExchangeListener listener;
	private final AtomicInteger size = new AtomicInteger(-1);
	private Throwable throwable;
 
	public InputStreamCaptor(InputStream in, StreamExchangeListener listener, boolean readContent) {
		this.in = in;
		this.listener = listener;
		this.out = readContent ? new ByteArrayOutputStream() : NULL_OUT;
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
			var v = remainingToRead(len);
			if(v > 0) {
				out.write(b, off, v);
			}
			size.getAndAdd(len);
		}
	}
	
	void cacheByte(int b) throws IOException {
		if(b > -1) {
			var v = remainingToRead(1);
			if(v > 0) {
				out.write(b);
			}
			size.getAndAdd(1);
		}
	}
	
	int remainingToRead(int n) {
        if(size.compareAndSet(-1, 0)) { //first read
        	listener.onTransmissionStart();
		}
        var len = size.get();
		return len < MAX_BYTES_TO_CAPTURE ? min(n, MAX_BYTES_TO_CAPTURE-len) : 0; // return remaining size
	}
	
	@Override
	public void close() throws IOException {
		if(out instanceof ByteArrayOutputStream) {
			out.close(); // safe close
		}
		try {
			in.close();
		}
		catch (Exception e) {
			throwable = e;
			throw e;
		}
		finally {
    		if(size.get() > -1) { //
            	listener.onTransmissionEnd();
    		}	
		}
	}
	
	public long transferedSize(){
		return size.get();
	}
	
	public byte[] transferedBytes() {
		return out instanceof ByteArrayOutputStream bos ? bos.toByteArray() : null;
	}
	
	@Override
	public Throwable throwable() {
		return throwable;
	}
}
