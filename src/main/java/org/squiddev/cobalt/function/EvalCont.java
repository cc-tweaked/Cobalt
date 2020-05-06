package org.squiddev.cobalt.function;

import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.debug.DebugFrame;

/**
 * Continuation object for partial evaluation.
 * Used to pack results of compiled instruction executions.
 */
public class EvalCont {
	/**
	 * Used to pass vararg return results.
	 */
	public Varargs varargs;
	/**
	 * Used together with {@link #function} to switch the current frame in calls and returns.
	 */
	public DebugFrame debugFrame;
	/**
	 * @see #debugFrame
	 */
	public LuaInterpretedFunction function;

	public EvalCont(Varargs v) {
		varargs = v;
	}

	public EvalCont(DebugFrame frame, LuaInterpretedFunction fn) {
		debugFrame = frame;
		function = fn;
	}
}
