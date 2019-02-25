package org.squiddev.cobalt;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugHelpers;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.ResumableVarArgFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.LuaLibrary;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_HOOKED;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_HOOKYIELD;

/**
 * Tests yielding in a whole load of places
 */
@RunWith(Parameterized.class)
public class CoroutineTest {
	private final String name;
	private ScriptDrivenHelpers helpers;

	public CoroutineTest(String name) {
		this.name = name;
		this.helpers = new ScriptDrivenHelpers("/coroutine/");
	}

	@Rule
	public Timeout globalTimeout = Timeout.seconds(1);

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> getTests() {
		Object[][] tests = {
			{"basic"},
			{"debug"},
			{"gsub"},
			{"ops"},
			{"pcall"},
			{"table"},
			{"tail"},
			{"xpcall"},
		};

		return Arrays.asList(tests);
	}

	private void setup() {
		helpers.globals.load(helpers.state, new Functions());
	}

	private void setBlockingYield() {
		((LuaTable) helpers.globals.rawget("coroutine")).rawset("yield", new VarArgFunction() {
			@Override
			public Varargs invoke(LuaState state, Varargs args) throws LuaError {
				try {
					return LuaThread.yieldBlocking(state, args);
				} catch (InterruptedException e) {
					throw new OrphanedThread();
				}
			}
		});
	}

	@Test
	public void run() throws IOException, CompileException, LuaError {
		helpers.setup();
		setup();
		LuaThread.runMain(helpers.state, helpers.loadScript(name));
	}

	@Test
	public void runSuspend() throws IOException, CompileException, LuaError {
		helpers.setup(x -> x.debug(new SuspendingDebug()));
		setup();

		LuaFunction function = helpers.loadScript(name);
		while (!helpers.state.getMainThread().getStatus().equals("dead")) {
			if (LuaThread.runMain(helpers.state, function) != null) break;
			function = null;
		}

		assertEquals("dead", helpers.state.getMainThread().getStatus());
	}

	@Test
	public void runBlocking() throws IOException, CompileException, LuaError {
		helpers.setup(LuaState.Builder::yieldThreader);
		setup();
		setBlockingYield();
		LuaThread.runMain(helpers.state, helpers.loadScript(name));
	}

	@Test
	public void runSuspendBlocking() throws IOException, CompileException, LuaError {
		helpers.setup(x -> x.debug(new SuspendingDebug()).yieldThreader());
		setup();
		setBlockingYield();

		LuaFunction function = helpers.loadScript(name);
		while (!helpers.state.getMainThread().getStatus().equals("dead")) {
			if (LuaThread.runMain(helpers.state, function) != null) break;
			function = null;
		}

		assertEquals("dead", helpers.state.getMainThread().getStatus());
	}

	private static class Functions extends ResumableVarArgFunction<LuaThread> implements LuaLibrary {
		@Override
		public LuaValue add(LuaState state, LuaTable environment) {
			bind(environment, Functions.class, new String[]{"suspend", "run", "assertEquals", "fail", "id"});
			return environment;
		}

		@Override
		public Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
			switch (opcode) {
				case 0: // suspend
					throw UnwindThrowable.suspend();
				case 1: { // run
					LuaThread thread = new LuaThread(state, args.first().checkFunction(), getfenv());
					di.state = thread;
					throw LuaThread.resume(state, thread, Constants.NONE);
				}
				case 2: { // asssertEquals
					String traceback = DebugHelpers.traceback(state.getCurrentThread(), 0);
					Assert.assertEquals(traceback, args.arg(1), args.arg(2));
					return Constants.NONE;
				}
				case 3: { // fail
					String traceback = DebugHelpers.traceback(state.getCurrentThread(), 0);
					Assert.fail(args.first().toString() + ":\n" + traceback);
					return Constants.NONE;
				}
				case 4: // id
					return args;
				default:
					return Constants.NONE;
			}
		}

		@Override
		public Varargs resumeThis(LuaState state, LuaThread thread, Varargs value) throws LuaError, UnwindThrowable {
			switch (opcode) {
				case 0:
					return value;
				case 1:
					if (!thread.isAlive()) return value;
					throw LuaThread.resume(state, thread, value);
				default:
					throw new NonResumableException("Cannot resume " + debugName());
			}
		}
	}

	private static class SuspendingDebug extends DebugHandler {
		private boolean suspend = true;

		private int flags;
		private boolean inHook;

		@Override
		public void onInstruction(DebugState ds, DebugFrame di, int pc) throws LuaError, UnwindThrowable {
			di.pc = pc;

			if (suspend) {
				// Save the current state
				flags = di.flags;
				inHook = ds.inhook;

				// Set HOOK_YIELD and HOOKED flags so we know its an instruction hook
				di.flags |= FLAG_HOOKYIELD | FLAG_HOOKED;

				// We don't want to suspend next tick.
				suspend = false;
				throw UnwindThrowable.suspend();
			} else {
				// Restore the old state
				ds.inhook = inHook;
				di.flags = flags;
				suspend = true;

				// And continue as normal
				super.onInstruction(ds, di, pc);
			}
		}
	}
}
