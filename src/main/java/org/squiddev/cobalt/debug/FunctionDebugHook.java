package org.squiddev.cobalt.debug;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LuaFunction;

import java.util.Objects;

public record FunctionDebugHook(LuaFunction function) implements DebugHook {
	public FunctionDebugHook {
		Objects.requireNonNull(function, "function cannot be null");
	}

	@Override
	public boolean inheritHook() {
		return false;
	}

	@Override
	public void onCall(LuaState state, DebugState ds, DebugFrame frame) throws LuaError, UnwindThrowable {
		OperationHelper.call(state, function, CALL);
	}

	@Override
	public void onReturn(LuaState state, DebugState ds, DebugFrame frame) throws LuaError, UnwindThrowable {
		OperationHelper.call(state, function, RETURN);
	}

	@Override
	public void onCount(LuaState state, DebugState ds, DebugFrame frame) throws LuaError, UnwindThrowable {
		OperationHelper.call(state, function, COUNT);
	}

	@Override
	public void onLine(LuaState state, DebugState ds, DebugFrame frame, int newLine) throws LuaError, UnwindThrowable {
		OperationHelper.call(state, function, LINE, ValueFactory.valueOf(newLine));
	}
}
