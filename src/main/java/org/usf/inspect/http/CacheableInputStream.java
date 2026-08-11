package org.usf.inspect.http;

import static java.lang.Math.min;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lombok.experimental.Delegate;

/**
 * Wraps an input stream and optionally caches up to 10 KB of read content.
 */
public final class CacheableInputStream extends InputStream implements ResponseContent {
	
	static final OutputStream NO_OUT = new OutputStream() { //nullOutputStream may throws Exception
		/**
		 * Ignores the supplied byte.
		 *
		 * @param b the byte to ignore
		 * @throws IOException if the write operation fails
		 */
		@Override
		public void write(int b) throws IOException {/* do nothing */}
	};
	
	private static final int MAX_SIZE = 10_000; //10k

	@Delegate
	private final InputStream in;
	private final OutputStream out;
	private int length;
  
	/**
	 * Creates a cacheable input stream wrapper.
	 *
	 * @param in the wrapped input stream
	 * @param cache whether response bytes should be cached
	 */
	public CacheableInputStream(InputStream in, boolean cache) {
		this.in = in;
		this.out = cache ? new ByteArrayOutputStream() : NO_OUT;
	}
	
	/**
	 * Reads the next byte from the wrapped stream and caches it when enabled.
	 *
	 * @return the next byte value, or {@code -1} at end of stream
	 * @throws IOException if reading fails
	 */
	@Override
	public int read() throws IOException {
		var b = in.read();
		cacheByte(b);
		return b;
	}
	
	/**
	 * Reads bytes into the supplied array and caches the consumed content when enabled.
	 *
	 * @param b the destination buffer
	 * @return the number of bytes read, or {@code -1} at end of stream
	 * @throws IOException if reading fails
	 */
	@Override
	public int read(byte[] b) throws IOException {
		var n = in.read(b);
		cacheBytes(b, 0, n);
		return n;
	}
	
	/**
	 * Reads bytes into a portion of the supplied array and caches the consumed content when enabled.
	 *
	 * @param b the destination buffer
	 * @param off the start offset in the array
	 * @param len the maximum number of bytes to read
	 * @return the number of bytes read, or {@code -1} at end of stream
	 * @throws IOException if reading fails
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		var n = in.read(b, off, len);
		cacheBytes(b, off, n);
		return n;
	}
	
	/**
	 * Reads up to the requested number of bytes into the supplied array and caches them when enabled.
	 *
	 * @param b the destination buffer
	 * @param off the start offset in the array
	 * @param len the maximum number of bytes to read
	 * @return the number of bytes read
	 * @throws IOException if reading fails
	 */
	@Override
	public int readNBytes(byte[] b, int off, int len) throws IOException {
		var n = in.readNBytes(b, off, len);
		cacheBytes(b, off, n);
		return n;
	}
	
	/**
	 * Reads all remaining bytes from the wrapped stream and caches them when enabled.
	 *
	 * @return the remaining bytes from the stream
	 * @throws IOException if reading fails
	 */
	@Override
	public byte[] readAllBytes() throws IOException {
		var b = in.readAllBytes();
		cacheBytes(b, 0, b.length);
		return b;
	}
	
	/**
	 * Reads up to the requested number of bytes from the wrapped stream and caches them when enabled.
	 *
	 * @param len the maximum number of bytes to read
	 * @return the bytes that were read
	 * @throws IOException if reading fails
	 */
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

	/**
	 * Return the remaining size in n (int).
	 * if there is no more size left, return 0
	 */
	int remainingCacheCapacity(int n) {
		return length < MAX_SIZE ? min(n, MAX_SIZE-length) : 0;
	}
	
	/**
	 * Closes the cache stream, when present, and then closes the wrapped input stream.
	 *
	 * @throws IOException if closing either stream fails
	 */
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
	
	/**
	 * Returns the total number of bytes read from the wrapped stream.
	 *
	 * @return the total content size in bytes
	 */
	public long contentSize(){
		return length;
	}
	
	/**
	 * Returns the cached bytes when caching is enabled.
	 *
	 * @return the cached bytes, or {@code null} when caching is disabled
	 */
	public byte[] contentBytes() {
		return out instanceof ByteArrayOutputStream bos ? bos.toByteArray() : null;
	}
}
