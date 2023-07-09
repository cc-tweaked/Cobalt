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
import org.squiddev.cobalt.debug.DebugState;

import static org.squiddev.cobalt.debug.DebugFrame.*;

public non-sealed abstract class ResumableVarArgFunction<T> extends LibFunction implements Resumable<Object> {
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
		DebugState ds = DebugState.get(state);

		// Push the frame
		DebugFrame di = ds.pushJavaInfo();
		di.setFunction(this, null);
		ds.onCall(di, args);

		Varargs result = invoke(state, di, args);
		onReturn(ds, di, result);
		return result;
	}

	@Override
	public final Varargs resume(LuaState state, DebugFrame frame, Object object, Varargs value) throws LuaError, UnwindThrowable {
		DebugState ds = DebugState.get(state);
		DebugFrame di = ds.getStackUnsafe();

		if ((di.flags & FLAG_CALL_HOOK) != 0) {
			// We yielded within the call hook: extract the arguments from the state and then execute.
			// TODO: Ideally we'd do this inside DebugFrame when resuming, but that requires a bigger change.
			assert ds.inhook;
			ds.inhook = false;
			frame.flags &= ~FLAG_CALL_HOOK;

			// Reset the state and invoke the main function
			Varargs result = invoke(state, di, di.extras);
			onReturn(ds, di, result);
			return result;
		} else if ((di.flags & FLAG_RETURN_HOOK) != 0) {
			// We yielded within the return hook, so now can just return normlly.
			assert ds.inhook;
			ds.inhook = false;
			frame.flags &= ~FLAG_RETURN_HOOK;

			// Just pop the frame
			Varargs args = di.extras;
			ds.onReturnNoHook();
			return args;
		} else {
			@SuppressWarnings("unchecked")
			Varargs result = resumeThis(state, (T) object, value);
			onReturn(ds, ds.getStackUnsafe(), result);
			return result;
		}
	}

	@Override
	public final Varargs resumeError(LuaState state, DebugFrame frame, Object object, LuaError error) throws LuaError, UnwindThrowable {
		if ((frame.flags & FLAG_ANY_HOOK) != 0) throw error;

		@SuppressWarnings("unchecked")
		Varargs result = resumeErrorThis(state, (T) object, error);

		DebugState ds = DebugState.get(state);
		onReturn(ds, ds.getStack(), result);
		return result;
	}

	protected abstract Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable;

	protected abstract Varargs resumeThis(LuaState state, T object, Varargs value) throws LuaError, UnwindThrowable;

	protected Varargs resumeErrorThis(LuaState state, T object, LuaError error) throws LuaError, UnwindThrowable {
		throw error;
	}

	private static void onReturn(DebugState ds, DebugFrame di, Varargs result) throws LuaError, UnwindThrowable {
		try {
			ds.onReturn(di);
		} catch (UnwindThrowable e) {
			di.extras = result;
			throw e;
		}
	}
}
