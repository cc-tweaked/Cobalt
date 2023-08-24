package org.squiddev.cobalt.vm;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaFunction;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
			LuaFunction function = LoadState.load(state, new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), "=input", new LuaTable());
			return a -> function.call(state, a);
		} catch (CompileException | IOException e) {
			throw new IllegalStateException("Failed to create operator with " + name, e);
		}
	}

	public static BinaryOperator createBinOp(LuaState state, String name) {
		try {
			var input = "local a, b = ... return a " + name + " b";
			LuaFunction function = LoadState.load(state, new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), "=input", new LuaTable());
			return (a, b) -> function.call(state, a, b);
		} catch (CompileException | IOException e) {
			throw new IllegalStateException("Failed to create operator with " + name, e);
		}
	}

	public static Comparison createComparison(LuaState state, String name) {
		var comparison = createBinOp(state, name);
		return (a, b) -> comparison.apply(a, b).toBoolean();
	}
}
