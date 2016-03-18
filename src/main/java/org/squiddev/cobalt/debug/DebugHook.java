package org.squiddev.cobalt.debug;

import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaString;

import static org.squiddev.cobalt.ValueFactory.valueOf;

/**
 * A basic method for registering for debug hooks
 */
public interface DebugHook {
	LuaString CALL = valueOf("call");
	LuaString LINE = valueOf("line");
	LuaString COUNT = valueOf("count");
	LuaString RETURN = valueOf("return");
	LuaString TAILRETURN = valueOf("tail return");

	/**
	 * Called after entering a function
	 *
	 * @param state Current lua state
	 * @param ds    The current debug state
	 * @param frame The current frame
	 */
	void onCall(LuaState state, DebugState ds, DebugFrame frame);

	/**
	 * Called before exiting a function
	 *
	 * @param state Current lua state
	 * @param ds    The current debug state
	 * @param frame The current frame
	 */
	void onReturn(LuaState state, DebugState ds, DebugFrame frame);

	/**
	 * Called before ever 'n' instructions
	 *
	 * @param state Current lua state
	 * @param ds    The current debug state
	 * @param frame The current frame
	 */
	void onCount(LuaState state, DebugState ds, DebugFrame frame);

	/**
	 * Called before each line changes
	 *
	 * @param state   Current lua state
	 * @param ds      The current debug state
	 * @param frame   The current frame
	 * @param oldLine The previous line
	 * @param newLine The new new line
	 */
	void onLine(LuaState state, DebugState ds, DebugFrame frame, int oldLine, int newLine);
}
