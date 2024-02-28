package org.squiddev.cobalt.vm;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.Dispatch;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public final class LuaOperators {
	private LuaOperators() {
	}

	public interface UnaryOperator {
		LuaValue apply(LuaValue value) throws LuaError, UnwindThrowable;
	}

	public interface BinaryOperator {
		LuaValue apply(LuaValue left, LuaValue right) throws LuaError, UnwindThrowable;
	}

	public interface Comparison {
		boolean apply(LuaValue left, LuaValue right) throws LuaError, UnwindThrowable;
	}

	public static UnaryOperator createUnOp(LuaState state, String name) {
		try {
			var input = "local a = ... return " + name + " a";
			var function = LoadState.load(state, new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), "=input", new LuaTable());
			return a -> Dispatch.call(state, function, a);
		} catch (CompileException | LuaError e) {
			throw new IllegalStateException("Failed to create operator with " + name, e);
		}
	}

	public static BinaryOperator createBinOp(LuaState state, String name) {
		try {
			var input = "local a, b = ... return a " + name + " b";
			var function = LoadState.load(state, new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), "=input", new LuaTable());
			return (a, b) -> Dispatch.call(state, function, a, b);
		} catch (CompileException | LuaError e) {
			throw new IllegalStateException("Failed to create operator with " + name, e);
		}
	}

	public static Comparison createComparison(LuaState state, String name) {
		var comparison = createBinOp(state, name);
		return (a, b) -> comparison.apply(a, b).toBoolean();
	}
}
