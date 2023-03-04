package org.squiddev.cobalt.function;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugFrame;

import java.util.function.Supplier;

/**
 * Utility class for registering multiple functions for a library. This behaves similarly to Lua's {@code luaL_Reg}.
 */
public class RegisteredFunction {
	private final String name;
	private final Supplier<LibFunction> factory;

	private RegisteredFunction(String name, Supplier<LibFunction> factory) {
		this.name = name;
		this.factory = factory;
	}

	public LibFunction create() {
		LibFunction function = factory.get();
		function.name = name;
		return function;
	}

	public static LuaTable bind(RegisteredFunction[] functions) {
		LuaTable table = new LuaTable(0, functions.length);
		bind(table, functions);
		return table;
	}

	public static void bind(LuaTable table, RegisteredFunction[] functions) {
		for (RegisteredFunction def : functions) {
			table.rawset(def.name, def.create());
		}
	}

	public static RegisteredFunction ofFactory(String name, Supplier<LibFunction> fn) {
		return new RegisteredFunction(name, fn);
	}

	public static RegisteredFunction of(String name, ZeroArgFunction.Signature fn) {
		return new RegisteredFunction(name, () -> new ZeroArgFunction() {
			@Override
			public LuaValue call(LuaState state) throws LuaError, UnwindThrowable {
				return fn.call(state);
			}
		});
	}

	public static RegisteredFunction of(String name, OneArgFunction.Signature fn) {
		return new RegisteredFunction(name, () -> new OneArgFunction() {
			@Override
			public LuaValue call(LuaState state, LuaValue arg) throws LuaError, UnwindThrowable {
				return fn.call(state, arg);
			}
		});
	}

	public static RegisteredFunction of(String name, TwoArgFunction.Signature fn) {
		return new RegisteredFunction(name, () -> new TwoArgFunction() {
			@Override
			public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError, UnwindThrowable {
				return fn.call(state, arg1, arg2);
			}
		});
	}

	public static RegisteredFunction of(String name, ThreeArgFunction.Signature fn) {
		return new RegisteredFunction(name, () -> new ThreeArgFunction() {
			@Override
			public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3) throws LuaError, UnwindThrowable {
				return fn.call(state, arg1, arg2, arg3);
			}
		});
	}

	public static RegisteredFunction ofV(String name, VarArgFunction.Signature fn) {
		return new RegisteredFunction(name, () -> new VarArgFunction() {
			@Override
			public Varargs invoke(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
				return fn.invoke(state, args);
			}
		});
	}

	public static RegisteredFunction ofS(String name, SuspendedVarArgFunction.Signature fn) {
		return new RegisteredFunction(name, () -> new SuspendedVarArgFunction() {
			@Override
			protected Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
				return fn.invoke(state, di, args);
			}
		});
	}
}
