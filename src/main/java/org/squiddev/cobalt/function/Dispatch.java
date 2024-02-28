package org.squiddev.cobalt.function;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugState;

import static org.squiddev.cobalt.debug.DebugFrame.FLAG_FRESH;

/**
 * Support for calling Lua functions (and other Lua values).
 */
public final class Dispatch {
	private Dispatch() {
	}

	public static LuaValue call(LuaState state, LuaValue function) throws LuaError, UnwindThrowable {
		return call(state, function, -1);
	}

	public static LuaValue call(LuaState state, LuaValue function, int stack) throws LuaError, UnwindThrowable {
		return function instanceof LuaFunction func
			? callImpl(state, func)
			: callImpl(state, getCallMetamethod(state, function, stack), function);
	}

	public static LuaValue call(LuaState state, LuaValue function, LuaValue arg) throws LuaError, UnwindThrowable {
		return call(state, function, arg, -1);
	}

	public static LuaValue call(LuaState state, LuaValue function, LuaValue arg, int stack) throws LuaError, UnwindThrowable {
		return function instanceof LuaFunction func
			? callImpl(state, func, arg)
			: callImpl(state, getCallMetamethod(state, function, stack), function, arg);
	}

	public static LuaValue call(LuaState state, LuaValue function, LuaValue arg1, LuaValue arg2) throws LuaError, UnwindThrowable {
		return call(state, function, arg1, arg2, -1);
	}

	public static LuaValue call(LuaState state, LuaValue function, LuaValue arg1, LuaValue arg2, int stack) throws LuaError, UnwindThrowable {
		return function instanceof LuaFunction func
			? callImpl(state, func, arg1, arg2)
			: callImpl(state, getCallMetamethod(state, function, stack), function, arg1, arg2);

	}

	public static LuaValue call(LuaState state, LuaValue function, LuaValue arg1, LuaValue arg2, LuaValue arg3) throws LuaError, UnwindThrowable {
		return call(state, function, arg1, arg2, arg3, -1);
	}

	public static LuaValue call(LuaState state, LuaValue function, LuaValue arg1, LuaValue arg2, LuaValue arg3, int stack) throws LuaError, UnwindThrowable {
		return function instanceof LuaFunction func
			? callImpl(state, func, arg1, arg2, arg3)
			: invokeImpl(state, getCallMetamethod(state, function, stack), ValueFactory.varargsOf(function, arg1, arg2, arg3)).first();
	}

	public static Varargs invoke(LuaState state, LuaValue function, Varargs args) throws LuaError, UnwindThrowable {
		return invoke(state, function, args, -1);
	}

	public static Varargs invoke(LuaState state, LuaValue function, Varargs args, int stack) throws LuaError, UnwindThrowable {
		return function instanceof LuaFunction func
			? invokeImpl(state, func, args)
			: invokeImpl(state, getCallMetamethod(state, function, stack), ValueFactory.varargsOf(function, args));
	}

	private static LuaValue callImpl(LuaState state, LuaFunction function) throws UnwindThrowable, LuaError {
		DebugState ds = DebugState.get(state);
		DebugFrame di = ds.pushJavaInfo();

		LuaValue result;
		if (function instanceof LuaInterpretedFunction closure) {
			LuaInterpreter.setupCall(ds, di, closure, FLAG_FRESH);
			result = LuaInterpreter.execute(state, di, closure).first();
		} else {
			di.func = function;
			try {
				ds.onCall(di);
			} catch (UnwindThrowable e) {
				di.extras = Constants.NONE;
				throw e;
			}
			result = ((LibFunction) function).call(state);
		}

		ds.onReturn(di, result);
		return result;
	}

	private static LuaValue callImpl(LuaState state, LuaFunction function, LuaValue arg1) throws UnwindThrowable, LuaError {
		DebugState ds = DebugState.get(state);
		DebugFrame di = ds.pushJavaInfo();

		LuaValue result;
		if (function instanceof LuaInterpretedFunction closure) {
			LuaInterpreter.setupCall(ds, di, closure, arg1, FLAG_FRESH);
			result = LuaInterpreter.execute(state, di, closure).first();
		} else {
			di.func = function;
			try {
				ds.onCall(di);
			} catch (UnwindThrowable e) {
				di.extras = arg1;
				throw e;
			}
			result = ((LibFunction) function).call(state, arg1);
		}

		ds.onReturn(di, result);
		return result;
	}

	private static LuaValue callImpl(LuaState state, LuaFunction function, LuaValue arg1, LuaValue arg2) throws UnwindThrowable, LuaError {
		DebugState ds = DebugState.get(state);
		DebugFrame di = ds.pushJavaInfo();

		LuaValue result;
		if (function instanceof LuaInterpretedFunction closure) {
			LuaInterpreter.setupCall(ds, di, closure, arg1, arg2, FLAG_FRESH);
			result = LuaInterpreter.execute(state, di, closure).first();
		} else {
			di.func = function;
			try {
				ds.onCall(di);
			} catch (UnwindThrowable e) {
				di.extras = ValueFactory.varargsOf(arg1, arg2);
				throw e;
			}
			result = ((LibFunction) function).call(state, arg1, arg2);
		}

		ds.onReturn(di, result);
		return result;
	}

	private static LuaValue callImpl(LuaState state, LuaFunction function, LuaValue arg1, LuaValue arg2, LuaValue arg3) throws UnwindThrowable, LuaError {
		DebugState ds = DebugState.get(state);
		DebugFrame di = ds.pushJavaInfo();

		LuaValue result;
		if (function instanceof LuaInterpretedFunction closure) {
			LuaInterpreter.setupCall(ds, di, closure, arg1, arg2, arg3, FLAG_FRESH);
			result = LuaInterpreter.execute(state, di, closure).first();
		} else {
			di.func = function;
			try {
				ds.onCall(di);
			} catch (UnwindThrowable e) {
				di.extras = ValueFactory.varargsOf(arg1, arg2, arg3);
				throw e;
			}
			result = ((LibFunction) function).call(state, arg1, arg2, arg3);
		}

		ds.onReturn(di, result);
		return result;
	}

	private static Varargs invokeImpl(LuaState state, LuaFunction function, Varargs args) throws UnwindThrowable, LuaError {
		DebugState ds = DebugState.get(state);
		DebugFrame di = ds.pushJavaInfo();

		Varargs result;
		if (function instanceof LuaInterpretedFunction closure) {
			LuaInterpreter.setupCall(ds, di, closure, args, FLAG_FRESH);
			result = LuaInterpreter.execute(state, di, closure);
		} else {
			di.func = function;
			try {
				ds.onCall(di);
			} catch (UnwindThrowable e) {
				di.extras = args;
				throw e;
			}
			result = ((LibFunction) function).invoke(state, args);
		}

		ds.onReturn(di, result);
		return result;
	}

	public static Varargs invokeFrame(LuaState state, DebugFrame frame) throws UnwindThrowable, LuaError {
		var function = frame.func;
		return function instanceof LuaInterpretedFunction c
			? LuaInterpreter.execute(state, frame, c)
			: ((LibFunction) function).invoke(state, frame.extras);
	}

	/**
	 * Get the {@code __call} metamethod for the given object, asserting it is a function.
	 *
	 * @param state The current Lua state.
	 * @param value The value to look up. This should <strong>NOT</strong> be a {@link LuaFunction}.
	 * @param stack The current stack slot, or {@code 0}.
	 * @return The function to invoke.
	 * @throws LuaError If there is no metamethod.
	 */
	public static LuaFunction getCallMetamethod(LuaState state, LuaValue value, int stack) throws LuaError {
		assert !(value instanceof LuaFunction);

		LuaValue func = value.metatag(state, Constants.CALL);
		if (!(func instanceof LuaFunction metaFunc)) {
			throw ErrorFactory.operandError(state, value, "call", stack);
		}

		return metaFunc;
	}
}
