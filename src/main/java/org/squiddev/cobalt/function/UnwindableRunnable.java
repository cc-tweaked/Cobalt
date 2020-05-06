package org.squiddev.cobalt.function;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.UnwindThrowable;

@FunctionalInterface
public interface UnwindableRunnable {
    void run() throws LuaError, UnwindThrowable;
}
