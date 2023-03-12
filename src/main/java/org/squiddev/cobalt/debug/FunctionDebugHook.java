package org.squiddev.cobalt.debug;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.UnwindThrowable;
import org.squiddev.cobalt.ValueFactory;
import org.squiddev.cobalt.function.LuaFunction;

import java.util.Objects;

public final class FunctionDebugHook implements DebugHook {
	private final LuaFunction function;

	public FunctionDebugHook(LuaFunction function) {
		Objects.requireNonNull(function, "function cannot be null");
		this.function = function;
	}

	public LuaFunction getFunction() {
		return function;
	}

	@Override
	public boolean inheritHook() {
		return false;
	}

	@Override
	public void onCall(LuaState state, DebugState ds, DebugFrame frame) throws LuaError, UnwindThrowable {
		function.call(state, CALL);
	}

	@Override
	public void onReturn(LuaState state, DebugState ds, DebugFrame frame) throws LuaError, UnwindThrowable {
		function.call(state, RETURN);
	}

	@Override
	public void onCount(LuaState state, DebugState ds, DebugFrame frame) throws LuaError, UnwindThrowable {
		function.call(state, COUNT);
	}

	@Override
	public void onLine(LuaState state, DebugState ds, DebugFrame frame, int newLine) throws LuaError, UnwindThrowable {
		function.call(state, LINE, ValueFactory.valueOf(newLine));
	}
}
