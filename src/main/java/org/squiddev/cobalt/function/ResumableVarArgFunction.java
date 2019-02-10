package org.squiddev.cobalt.function;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugState;

import static org.squiddev.cobalt.debug.DebugFrame.FLAG_HOOKED;

public abstract class ResumableVarArgFunction<T> extends LibFunction implements Resumable<Object> {
	private static final Object CALL_MARKER = new Object();
	private static final Object RETURN_MARKER = new Object();

	@Override
	public final LuaValue call(LuaState state) throws LuaError, UnwindThrowable {
		return invoke(state, Constants.NONE).first();
	}

	@Override
	public final LuaValue call(LuaState state, LuaValue arg) throws LuaError, UnwindThrowable {
		return invoke(state, arg).first();
	}

	@Override
	public final LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError, UnwindThrowable {
		return invoke(state, ValueFactory.varargsOf(arg1, arg2)).first();
	}

	@Override
	public final LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3) throws LuaError, UnwindThrowable {
		return invoke(state, ValueFactory.varargsOf(arg1, arg2, arg3)).first();
	}

	@Override
	public final Varargs invoke(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
		DebugHandler debug = state.debug;
		DebugState ds = DebugHandler.getDebugState(state);

		// Push the frame
		DebugFrame di = debug.setupCall(ds, this, null);
		if (!ds.inhook && ds.hookcall) {
			try {
				ds.hookCall(di);
			} catch (UnwindThrowable e) {
				di.state = CALL_MARKER;
				di.extras = args;
				throw e;
			}
		}

		Varargs result = invoke(state, di, args);
		onReturn(debug, ds, di, result);
		return result;
	}

	@Override
	public final Varargs resume(LuaState state, Object object, Varargs value) throws LuaError, UnwindThrowable {
		DebugState ds = DebugHandler.getDebugState(state);

		if (object == CALL_MARKER) {
			// We yielded within the call hook: extract the arguments from the state.
			DebugFrame di = ds.getStackUnsafe();
			if (ds.inhook && (di.flags & FLAG_HOOKED) != 0) {
				ds.inhook = false;
				di.flags ^= FLAG_HOOKED;
			}

			// Reset the state and invoke the main function
			di.state = null;
			Varargs result = invoke(state, di, di.extras);
			onReturn(state.debug, ds, di, result);
			return result;
		} else if (object == RETURN_MARKER) {
			// We yielded within the return hook: just return without calling the hook.
			DebugFrame di = ds.getStackUnsafe();
			if (ds.inhook && (di.flags & FLAG_HOOKED) != 0) ds.inhook = false;

			// Just pop the frame
			Varargs args = di.extras;
			state.debug.onReturnError(ds);
			return args;
		} else {
			@SuppressWarnings("unchecked")
			Varargs result = resumeThis(state, (T) object, value);
			onReturn(state.debug, ds, ds.getStackUnsafe(), result);
			return result;
		}
	}

	@Override
	public final Varargs resumeError(LuaState state, Object object, LuaError error) throws LuaError, UnwindThrowable {
		if (object == RETURN_MARKER || object == CALL_MARKER) throw error;

		@SuppressWarnings("unchecked")
		Varargs result = resumeErrorThis(state, (T) object, error);

		DebugState ds = DebugHandler.getDebugState(state);
		onReturn(state.debug, ds, ds.getStack(), result);
		return result;
	}

	protected abstract Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable;

	protected abstract Varargs resumeThis(LuaState state, T object, Varargs value) throws LuaError, UnwindThrowable;

	protected Varargs resumeErrorThis(LuaState state, T object, LuaError error) throws LuaError, UnwindThrowable {
		throw error;
	}

	private static void onReturn(DebugHandler debug, DebugState ds, DebugFrame di, Varargs result) throws LuaError, UnwindThrowable {
		try {
			debug.onReturn(ds, di);
		} catch (UnwindThrowable e) {
			di.state = RETURN_MARKER;
			di.extras = result;
			throw e;
		}
	}
}
