package org.squiddev.cobalt.function;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.UnwindThrowable;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.unwind.SuspendedFunction;
import org.squiddev.cobalt.unwind.SuspendedTask;

/**
 * A {@link ResumableVarArgFunction} implementation which works with {@link SuspendedTask}/{@link SuspendedFunction}.
 */
public abstract class SuspendedVarArgFunction extends ResumableVarArgFunction<SuspendedTask<Varargs>> {
	@Override
	protected final Varargs resumeThis(LuaState state, SuspendedTask<Varargs> object, Varargs value) throws LuaError, UnwindThrowable {
		return object.resume(value);
	}
}
