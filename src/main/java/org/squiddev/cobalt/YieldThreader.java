package org.squiddev.cobalt;

import java.util.concurrent.Executor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class YieldThreader implements Executor {
	boolean abandoned;

	private final Executor executor;
	final Lock lock = new ReentrantLock();
	final Condition loop = lock.newCondition();

	volatile boolean running;
	volatile Varargs args;
	volatile private Throwable error;

	YieldThreader(Executor coroutineExecutor) {
		this.executor = coroutineExecutor;
	}

	@Override
	public void execute(Runnable command) {
		executor.execute(command);
	}

	public Varargs unpack() throws LuaError {
		// And extract the arguments again
		Varargs result = args;
		Throwable error = this.error;
		args = null;
		this.error = null;

		if (error != null) {
			rethrow(error);
			throw (LuaError) error;
		}

		return result;
	}

	public void set(Throwable error) {
		this.args = null;
		this.error = error;
	}

	public void set(Varargs args) {
		this.args = args;
		this.error = null;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> void rethrow(Throwable e) throws T {
		throw (T) e;
	}
}
