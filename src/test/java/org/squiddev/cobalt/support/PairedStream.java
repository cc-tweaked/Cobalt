package org.squiddev.cobalt.support;

import java.io.*;
import java.util.concurrent.*;

/**
 * A paired input and ouput stream. Thus operates in two possible modes:
 *
 * <ul>
 *     <li>"Sequential" - this just writes to a byte array and then reads from it.</li>
 *     <li>"Lockstep": Runs the writer and reader at the same time, and ensure read and write operations happen in sync
 *     with each other. This is useful when debugging, but obviously has much higher overhead.</li>
 * </ul>
 */
public class PairedStream {
	private static final boolean DEBUG = false;

	private final BlockingQueue<Integer> value = new ArrayBlockingQueue<>(1);
	private boolean closed = false;

	private final OutputStream writer = new OutputStream() {
		@Override
		public void write(int b) throws IOException {
			if (b < 0 || b > 255) throw new IllegalStateException("Byte out of bounds");
			try {
				value.put(b);
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
		}

		@Override
		public void close() throws IOException {
			super.close();
			try {
				value.put(-1);
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
		}
	};

	private final InputStream reader = new InputStream() {
		@Override
		public int read() throws IOException {
			if (closed) return -1;

			try {
				int read = value.take();
				if (read == -1) closed = true;
				return read;
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
		}
	};

	public static <T> T run(UncheckedConsumer<OutputStream> write, UncheckedFunction<InputStream, T> read) throws Exception {
		return DEBUG ? runLockstep(write, read) : runSequential(write, read);
	}

	public static <T> T runSequential(UncheckedConsumer<OutputStream> write, UncheckedFunction<InputStream, T> read) throws Exception {
		byte[] contents;
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			write.accept(output);
			contents = output.toByteArray();
		}

		try (ByteArrayInputStream input = new ByteArrayInputStream(contents)) {
			return read.apply(input);
		}
	}

	public static <T> T runLockstep(UncheckedConsumer<OutputStream> write, UncheckedFunction<InputStream, T> read) throws Exception {
		ExecutorService executor = Executors.newCachedThreadPool();
		PairedStream stream = new PairedStream();
		CompletableFuture<?> writer = CompletableFuture.runAsync(() -> {
			try {
				write.accept(stream.writer);
			} catch (Exception e) {
				doSneakyThrow(e);
			} finally {
				try {
					stream.writer.close();
				} catch (IOException ignored) {
				}
			}
		}, executor);
		CompletableFuture<T> reader = CompletableFuture.supplyAsync(() -> {
			try {
				return read.apply(stream.reader);
			} catch (Exception e) {
				doSneakyThrow(e);
				return null;
			}
		}, executor);

		CompletableFuture.anyOf(reader, writer).get();
		T value = reader.get();
		executor.shutdownNow();
		return value;
	}

	public interface UncheckedConsumer<T> {
		void accept(T value) throws Exception;
	}

	public interface UncheckedFunction<T, U> {
		U apply(T value) throws Exception;
	}

	public static <E extends Throwable> E sneakyThrow(Throwable e) throws E {
		throw (E) e;
	}

	public static void doSneakyThrow(Throwable e) {
		PairedStream.<RuntimeException>sneakyThrow(e);
	}
}
