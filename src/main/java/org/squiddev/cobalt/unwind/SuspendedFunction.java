package org.squiddev.cobalt.unwind;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.UnwindThrowable;

/**
 * A function which may be called once, and then later treated as a {@link SuspendedTask suspended task}.
 *
 * @param <T> The result type of this function.
 */
public interface SuspendedFunction<T> extends SuspendedTask<T> {
	T call(LuaState state) throws LuaError, UnwindThrowable;
}
