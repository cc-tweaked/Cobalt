package org.squiddev.cobalt.support;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaThread;
import org.squiddev.cobalt.UnwindThrowable;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugState;

import static org.squiddev.cobalt.debug.DebugFrame.FLAG_HOOKED;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_HOOKYIELD;

public class SuspendingDebug extends DebugHandler {
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
			LuaThread.suspend(ds.getLuaState());
		}

		// Restore the old state
		ds.inhook = inHook;
		di.flags = flags;
		suspend = true;

		// And continue as normal
		super.onInstruction(ds, di, pc);
	}

	public DebugHandler justResume() {
		return suspend ? new DebugHandler() : new Resuming(flags, inHook);
	}

	private static class Resuming extends DebugHandler {
		private boolean needsWork = true;
		private final int flags;
		private final boolean inHook;

		private Resuming(int flags, boolean inHook) {
			this.flags = flags;
			this.inHook = inHook;
		}

		@Override
		public void onInstruction(DebugState ds, DebugFrame di, int pc) throws LuaError, UnwindThrowable {
			if (needsWork) {
				ds.inhook = inHook;
				di.flags = flags;
				needsWork = false;
			}

			super.onInstruction(ds, di, pc);
		}
	}
}
