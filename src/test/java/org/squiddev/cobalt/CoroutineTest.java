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

import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHelpers;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.RegisteredFunction;
import org.squiddev.cobalt.function.ResumableVarArgFunction;
import org.squiddev.cobalt.interrupt.InterruptAction;
import org.squiddev.cobalt.interrupt.InterruptHandler;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests yielding in a whole load of places
 */
@Timeout(1)
public class CoroutineTest {
	private final ScriptHelper helpers = new ScriptHelper("/coroutine/");

	public static String[] getTests() {
		return new String[]{
			"basic", "debug", "gsub", "load", "ops", "pcall", "table", "tail", "xpcall",
		};
	}

	private void addGlobals() {
		RegisteredFunction.bind(helpers.globals, new RegisteredFunction[]{
			RegisteredFunction.ofFactory("run", Run::new),
			RegisteredFunction.ofV("assertEquals", CoroutineTest::assertEquals$),
			RegisteredFunction.ofV("fail", CoroutineTest::fail$),
			RegisteredFunction.ofV("id", CoroutineTest::id),
		});
	}

	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@MethodSource("getTests")
	public void run(String name) throws IOException, CompileException, LuaError, InterruptedException {
		helpers.setup();
		addGlobals();
		LuaThread.runMain(helpers.state, helpers.loadScript(name));
	}

	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@MethodSource("getTests")
	public void runSuspend(String name) throws IOException, CompileException, LuaError, InterruptedException {
		var handler = new SuspendingHandler();
		helpers.setup(x -> x.interruptHandler(handler));
		handler.state = helpers.state;
		addGlobals();
		helpers.state.interrupt();

		LuaFunction function = helpers.loadScript(name);
		Varargs result = LuaThread.runMain(helpers.state, function);
		while (result == null && helpers.state.getMainThread().isAlive()) {
			result = LuaThread.run(helpers.state.getCurrentThread(), Constants.NONE);
		}

		assertEquals("dead", helpers.state.getMainThread().getStatus().getDisplayName());
	}

	private static class Run extends ResumableVarArgFunction<LuaThread> {
		@Override
		protected Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
			LuaThread thread = new LuaThread(state, args.first().checkFunction());
			di.state = thread;
			Varargs value = Constants.NONE;
			while (thread.isAlive()) value = LuaThread.resume(state, thread, value);
			return value;
		}

		@Override
		public Varargs resume(LuaState state, LuaThread thread, Varargs value) throws LuaError, UnwindThrowable {
			while (thread.isAlive()) value = LuaThread.resume(state, thread, value);
			return value;
		}
	}

	private static Varargs assertEquals$(LuaState state, Varargs args) {
		String traceback = DebugHelpers.traceback(state.getCurrentThread(), 0);
		assertEquals(args.arg(1), args.arg(2), traceback);
		return Constants.NONE;
	}

	private static Varargs fail$(LuaState state, Varargs args) {
		String traceback = DebugHelpers.traceback(state.getCurrentThread(), 0);
		fail(args.first().toString() + ":\n" + traceback);
		return Constants.NONE;
	}

	private static Varargs id(LuaState state, Varargs args) {
		return args;
	}

	private static class SuspendingHandler implements InterruptHandler {
		LuaState state;
		private boolean suspend = true;

		@Override
		public InterruptAction interrupted() {
			state.interrupt();
			suspend = !suspend;
			return suspend ? InterruptAction.CONTINUE : InterruptAction.SUSPEND;
		}
	}
}
