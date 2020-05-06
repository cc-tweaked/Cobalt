package org.squiddev.cobalt.function;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.UnwindThrowable;

@FunctionalInterface
public interface UnwindableCallable<R> {
    R call() throws LuaError, UnwindThrowable;
}
