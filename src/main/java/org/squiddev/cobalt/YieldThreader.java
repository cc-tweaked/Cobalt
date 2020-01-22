/*
 * The MIT License (MIT)
 *
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2020 SquidDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.squiddev.cobalt;

import java.util.concurrent.Executor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class YieldThreader implements Executor {
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

	Varargs unpack() throws LuaError {
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

	void set(Throwable error) {
		this.args = null;
		this.error = error;
	}

	void set(Varargs args) {
		this.args = args;
		this.error = null;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> void rethrow(Throwable e) throws T {
		throw (T) e;
	}
}
