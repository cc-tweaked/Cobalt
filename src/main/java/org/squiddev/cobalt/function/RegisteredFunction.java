package org.squiddev.cobalt.function;

import org.squiddev.cobalt.*;

/**
 * Utility class for registering multiple functions for a library. This behaves similarly to Lua's {@code luaL_Reg}.
 */
public class RegisteredFunction {
	private final String name;
	private final LibFunction function;

	private RegisteredFunction(String name, LibFunction function) {
		this.name = name;
		this.function = function;
	}

	public LibFunction create(LuaTable env) {
		function.name = name;
		function.env = env;
		return function;
	}

	public static void bind(LuaTable env, LuaTable table, RegisteredFunction[] functions) {
		for (RegisteredFunction def : functions) {
			table.rawset(def.name, def.create(env));
		}
	}

	public static RegisteredFunction of(String name, LibFunction fn) {
		return new RegisteredFunction(name, fn);
	}

	public static RegisteredFunction of(String name, ZeroArgFunction.Signature fn) {
		return new RegisteredFunction(name, new ZeroArgFunction() {
			@Override
			public LuaValue call(LuaState state) throws LuaError, UnwindThrowable {
				return fn.call(state);
			}
		});
	}

	public static RegisteredFunction of(String name, OneArgFunction.Signature fn) {
		return new RegisteredFunction(name, new OneArgFunction() {
			@Override
			public LuaValue call(LuaState state, LuaValue arg) throws LuaError, UnwindThrowable {
				return fn.call(state, arg);
			}
		});
	}

	public static RegisteredFunction of(String name, TwoArgFunction.Signature fn) {
		return new RegisteredFunction(name, new TwoArgFunction() {
			@Override
			public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError, UnwindThrowable {
				return fn.call(state, arg1, arg2);
			}
		});
	}

	public static RegisteredFunction of(String name, ThreeArgFunction.Signature fn) {
		return new RegisteredFunction(name, new ThreeArgFunction() {
			@Override
			public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3) throws LuaError, UnwindThrowable {
				return fn.call(state, arg1, arg2, arg3);
			}
		});
	}

	public static RegisteredFunction ofV(String name, VarArgFunction.Signature fn) {
		return new RegisteredFunction(name, new VarArgFunction() {
			@Override
			public Varargs invoke(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
				return fn.invoke(state, args);
			}
		});
	}
}
