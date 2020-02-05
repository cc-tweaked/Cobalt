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
package org.squiddev.cobalt.function;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugState;

import static org.squiddev.cobalt.debug.DebugFrame.FLAG_HOOKED;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_JAVA;

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
		DebugState ds = DebugHandler.getDebugState(state);

		// Push the frame
		DebugFrame di = ds.pushJavaInfo();
		di.setFunction(this, null);
		di.flags |= FLAG_JAVA;
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
		onReturn(state.debug, ds, di, result);
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
