/*
 * ****************************************************************************
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2016 SquidDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ****************************************************************************
 */
package org.squiddev.cobalt.lib;


import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.jse.JsePlatform;

import static org.squiddev.cobalt.ValueFactory.valueOf;

/**
 * Subclass of {@link LibFunction} which implements the lua standard {@code coroutine}
 * library.
 *
 * The coroutine library in luaj has the same behavior as the
 * coroutine library in C, but is implemented using Java Threads to maintain
 * the call state between invocations.  Therefore it can be yielded from anywhere,
 * similar to the "Coco" yield-from-anywhere patch available for C-based lua.
 * However, coroutines that are yielded but never resumed to complete their execution
 * may not be collected by the garbage collector.
 *
 * @see LibFunction
 * @see JsePlatform
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.2">http://www.lua.org/manual/5.1/manual.html#5.2</a>
 */
public class CoroutineLib extends VarArgFunction {

	private static final int INIT = 0;
	private static final int CREATE = 1;
	private static final int RESUME = 2;
	private static final int RUNNING = 3;
	private static final int STATUS = 4;
	private static final int YIELD = 5;
	private static final int WRAP = 6;
	private static final int WRAPPED = 7;

	private LuaTable init(LuaState state) {
		LuaTable t = new LuaTable();
		bind(state, t, CoroutineLib.class, new String[]{
				"create", "resume", "running", "status", "yield", "wrap"},
			CREATE);
		env.set(state, "coroutine", t);
		state.loadedPackages.set(state, "coroutine", t);
		return t;
	}

	@Override
	public Varargs invoke(LuaState state, Varargs args) {
		switch (opcode) {
			case INIT: {
				return init(state);
			}
			case CREATE: {
				final LuaValue func = args.arg(1).checkFunction();
				return new LuaThread(state, func, state.getCurrentThread().getfenv());
			}
			case RESUME: {
				final LuaThread t = args.arg(1).checkThread();
				return t.resume(args.subargs(2));
			}
			case RUNNING: {
				final LuaThread r = state.getCurrentThread();
				return r.isMainThread() ? Constants.NIL : r;
			}
			case STATUS: {
				return valueOf(args.arg(1).checkThread().getStatus());
			}
			case YIELD: {
				return LuaThread.yield(state, args);
			}
			case WRAP: {
				final LuaValue func = args.arg(1).checkFunction();
				final LuaThread thread = new LuaThread(state, func, func.getfenv());
				CoroutineLib cl = new CoroutineLib();
				cl.setfenv(thread);
				cl.name = "wrapped";
				cl.opcode = WRAPPED;
				return cl;
			}
			case WRAPPED: {
				final LuaThread t = (LuaThread) env;
				final Varargs result = t.resume(args);
				if (result.first().toBoolean()) {
					return result.subargs(2);
				} else {
					throw new LuaError(result.arg(2));
				}
			}
			default:
				return Constants.NONE;
		}
	}
}
