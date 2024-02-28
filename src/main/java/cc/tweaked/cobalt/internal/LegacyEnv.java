package cc.tweaked.cobalt.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Prototype;
import org.squiddev.cobalt.debug.Upvalue;
import org.squiddev.cobalt.function.LuaClosure;

import java.util.Objects;

/**
 * Utilities for working with Lua 5.1-style {@code getfenv}/{@code setfenv}.
 * <p>
 * These simply search for an {@link Constants#ENV _ENV} upvalue and set it.
 */
public final class LegacyEnv {
	private LegacyEnv() {
	}

	private static int findEnv(Prototype prototype) {
		for (int i = 0; i < prototype.upvalues(); i++) {
			if (Objects.equals(prototype.getUpvalueName(i), Constants.ENV)) return i;
		}

		return -1;
	}

	public static @Nullable LuaTable getEnv(LuaClosure closure) {
		int index = findEnv(closure.getPrototype());
		return index >= 0 && closure.getUpvalue(index).getValue() instanceof LuaTable t ? t : null;
	}

	public static @Nullable LuaTable getEnv(LuaValue value) {
		return value instanceof LuaClosure c ? getEnv(c) : null;
	}

	public static void setEnv(LuaClosure closure, LuaTable env) {
		int index = findEnv(closure.getPrototype());
		if (index >= 0) {
			// Slightly odd to create a new upvalue here, but ensures that it only affects this function.
			closure.setUpvalue(index, new Upvalue(env));
		}
	}

	public static boolean setEnv(LuaValue value, LuaTable env) {
		if (!(value instanceof LuaClosure c)) return false;

		setEnv(c, env);
		// We always return true on Lua closures, even if technically this won't do anything, as it ensures somewhat
		// consistent behaviour.
		return true;
	}
}
