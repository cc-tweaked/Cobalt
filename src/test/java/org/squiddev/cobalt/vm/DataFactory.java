package org.squiddev.cobalt.vm;

import org.squiddev.cobalt.Prototype;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LuaC;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public final class DataFactory {
	private DataFactory() {
	}

	public static Prototype prototype() {
		try {
			return LuaC.compile(new ByteArrayInputStream(new byte[]{}), "=prototype");
		} catch (CompileException | IOException e) {
			throw new RuntimeException(e);
		}
	}
}
