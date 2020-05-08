package org.squiddev.cobalt.function;

import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.debug.DebugFrame;

/**
 * Continuation object for partial evaluation.
 * Used to pack results of compiled instruction executions.
 */
public final class EvalCont {
	/**
	 * Used as the new value for the program counter.
	 */
	public final int programCounter;
	/**
	 * Used to pass vararg return results.
	 */
	public final Varargs varargs;
	/**
	 * Used together with {@link #function} to switch the current frame in calls and returns.
	 */
	public final DebugFrame debugFrame;
	/**
	 * @see #debugFrame
	 */
	public final LuaInterpretedFunction function;

	public EvalCont(int pc) {
		programCounter = pc;
		function = null;
		debugFrame = null;
		varargs = null;
	}

	public EvalCont(Varargs v) {
		programCounter = -3;
		varargs = v;
		function = null;
		debugFrame = null;
	}

	public EvalCont(DebugFrame frame, LuaInterpretedFunction fn) {
		programCounter = -4;
		debugFrame = frame;
		function = fn;
		varargs = null;
	}
}
