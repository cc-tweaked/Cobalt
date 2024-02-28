package org.squiddev.cobalt.vm;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Prototype;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LuaC;
import org.squiddev.cobalt.function.LuaInterpretedFunction;

import java.io.ByteArrayInputStream;

public final class DataFactory {
	private DataFactory() {
	}

	public static Prototype prototype(LuaState state) {
		try {
			return LuaC.compile(state, new ByteArrayInputStream(new byte[]{}), "=prototype");
		} catch (CompileException | LuaError e) {
			throw new RuntimeException(e);
		}
	}

	public static LuaInterpretedFunction closure(LuaState state) {
		return new LuaInterpretedFunction(prototype(state));
	}
}
