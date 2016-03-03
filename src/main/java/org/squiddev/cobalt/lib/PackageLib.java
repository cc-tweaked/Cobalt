/**
 * ****************************************************************************
 * Copyright (c) 2010-2011 Luaj.org. All rights reserved.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ****************************************************************************
 */
package org.squiddev.cobalt.lib;


import org.squiddev.cobalt.*;

import java.io.InputStream;

import static org.squiddev.cobalt.ValueFactory.varargsOf;

/**
 * Subclass of {@link LibFunction} which implements the lua standard package and module
 * library functions.
 * <p>
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 * However, the default filesystem search semantics are different and delegated to the bas library
 * as outlined in the {@link BaseLib}.
 *
 * @see LibFunction
 * @see BaseLib
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.3">http://www.lua.org/manual/5.1/manual.html#5.3</a>
 */
public class PackageLib extends OneArgFunction {
	public static final String DEFAULT_LUA_PATH = "?.lua";

	public LuaTable PACKAGE;

	/**
	 * Loader that loads from preload table if found there
	 */
	public LuaValue preload_loader;

	/**
	 * Loader that loads as a lua script using the LUA_PATH
	 */
	public LuaValue lua_loader;

	/**
	 * Loader that loads as a Java class.  Class must have public constructor and be a LuaValue
	 */
	public LuaValue java_loader;

	private static final LuaString _M = ValueFactory.valueOf("_M");
	private static final LuaString _NAME = ValueFactory.valueOf("_NAME");
	private static final LuaString _PACKAGE = ValueFactory.valueOf("_PACKAGE");
	private static final LuaString _DOT = ValueFactory.valueOf(".");
	private static final LuaString _LOADERS = ValueFactory.valueOf("loaders");
	private static final LuaString _LOADED = ValueFactory.valueOf("loaded");
	private static final LuaString _LOADLIB = ValueFactory.valueOf("loadlib");
	private static final LuaString _PRELOAD = ValueFactory.valueOf("preload");
	private static final LuaString _PATH = ValueFactory.valueOf("path");
	private static final LuaString _SEEALL = ValueFactory.valueOf("seeall");
	private static final LuaString _SENTINEL = ValueFactory.valueOf("\u0001");

	private static final int OP_MODULE = 0;
	private static final int OP_REQUIRE = 1;
	private static final int OP_LOADLIB = 2;
	private static final int OP_SEEALL = 3;
	private static final int OP_PRELOAD_LOADER = 4;
	private static final int OP_LUA_LOADER = 5;
	private static final int OP_JAVA_LOADER = 6;

	@Override
	public LuaValue call(LuaState state, LuaValue arg) {
		env.set(state, "require", new PkgLib1(env, "require", OP_REQUIRE, this));
		env.set(state, "module", new PkgLibV(env, "module", OP_MODULE, this));
		env.set(state, "package", PACKAGE = ValueFactory.tableOf(new LuaValue[]{
			_LOADED, state.loadedPackages,
			_PRELOAD, ValueFactory.tableOf(),
			_PATH, ValueFactory.valueOf(DEFAULT_LUA_PATH),
			_LOADLIB, new PkgLibV(env, "loadlib", OP_LOADLIB, this),
			_SEEALL, new PkgLib1(env, "seeall", OP_SEEALL, this),
			_LOADERS, ValueFactory.listOf(new LuaValue[]{
			preload_loader = new PkgLibV(env, "preload_loader", OP_PRELOAD_LOADER, this),
			lua_loader = new PkgLibV(env, "lua_loader", OP_LUA_LOADER, this),
			java_loader = new PkgLibV(env, "java_loader", OP_JAVA_LOADER, this),
		})}));
		state.loadedPackages.set(state, "package", PACKAGE);
		return env;
	}

	static final class PkgLib1 extends OneArgFunction {
		PackageLib lib;

		public PkgLib1(LuaValue env, String name, int opcode, PackageLib lib) {
			this.env = env;
			this.name = name;
			this.opcode = opcode;
			this.lib = lib;
		}

		@Override
		public LuaValue call(LuaState state, LuaValue arg) {
			switch (opcode) {
				case OP_REQUIRE:
					return lib.require(state, arg);
				case OP_SEEALL: {
					LuaTable t = arg.checkTable();
					LuaValue m = t.getMetatable(state);
					if (m == null) {
						t.setMetatable(state, m = ValueFactory.tableOf());
					}
					m.set(state, Constants.INDEX, state.getCurrentThread().getfenv());
					return Constants.NONE;
				}
			}
			return Constants.NIL;
		}
	}

	static final class PkgLibV extends VarArgFunction {
		PackageLib lib;

		public PkgLibV(LuaValue env, String name, int opcode, PackageLib lib) {
			this.env = env;
			this.name = name;
			this.opcode = opcode;
			this.lib = lib;
		}

		@Override
		public Varargs invoke(LuaState state, Varargs args) {
			switch (opcode) {
				case OP_MODULE:
					return lib.module(state, args);
				case OP_LOADLIB:
					return loadlib(args);
				case OP_PRELOAD_LOADER: {
					return lib.loader_preload(state, args);
				}
				case OP_LUA_LOADER: {
					return lib.loader_Lua(state, args);
				}
				case OP_JAVA_LOADER: {
					return lib.loader_Java(args);
				}
			}
			return Constants.NONE;
		}
	}

	/**
	 * Allow packages to mark themselves as loaded
	 *
	 * @param state The current lua state
	 * @param name  Name of package
	 * @param value Value of package
	 */
	public void setIsLoaded(LuaState state, String name, LuaTable value) {
		state.loadedPackages.set(state, name, value);
	}

	public void setLuaPath(LuaState state, String newLuaPath) {
		PACKAGE.set(state, _PATH, ValueFactory.valueOf(newLuaPath));
	}

	@Override
	public String toString() {
		return "package";
	}


	// ======================== Module, Package loading =============================

	/**
	 * module (name [, ...])
	 * <p>
	 * Creates a module. If there is a table in package.loaded[name], this table
	 * is the module. Otherwise, if there is a global table t with the given
	 * name, this table is the module. Otherwise creates a new table t and sets
	 * it as the value of the global name and the value of package.loaded[name].
	 * This function also initializes t._NAME with the given name, t._M with the
	 * module (t itself), and t._PACKAGE with the package name (the full module
	 * name minus last component; see below). Finally, module sets t as the new
	 * environment of the current function and the new value of
	 * package.loaded[name], so that require returns t.
	 * <p>
	 * If name is a compound name (that is, one with components separated by
	 * dots), module creates (or reuses, if they already exist) tables for each
	 * component. For instance, if name is a.b.c, then module stores the module
	 * table in field c of field b of global a.
	 * <p>
	 * This function may receive optional options after the module name, where
	 * each option is a function to be applied over the module.
	 *
	 * @param state The current lua state
	 * @param args  The arguments to set it up with
	 * @return {@link Constants#NONE}
	 */
	public Varargs module(LuaState state, Varargs args) {
		LuaString modname = args.arg(1).checkLuaString();
		int n = args.count();
		LuaValue value = state.loadedPackages.get(state, modname);
		LuaValue module;
		if (!value.isTable()) { /* not found? */

		    /* try global variable (and create one if it does not exist) */
			LuaValue globals = state.getCurrentThread().getfenv();
			module = findtable(state, globals, modname);
			if (module == null) {
				LuaValue result;
				throw new LuaError("name conflict for module '" + modname + "'");
			}
			state.loadedPackages.set(state, modname, module);
		} else {
			module = value;
		}


		/* check whether table already has a _NAME field */
		LuaValue name = module.get(state, _NAME);
		if (name.isNil()) {
			modinit(state, module, modname);
		}

		// set the environment of the current function
		LuaFunction f = LuaThread.getCallstackFunction(state, 1);
		if (f == null) {
			LuaValue result;
			throw new LuaError("no calling function");
		}
		if (!f.isClosure()) {
			LuaValue result;
			throw new LuaError("'module' not called from a Lua function");
		}
		f.setfenv(module);

		// apply the functions
		for (int i = 2; i <= n; i++) {
			args.arg(i).call(state, module);
		}

		// returns no results
		return Constants.NONE;
	}

	/**
	 * @param state The current lua state
	 * @param table the table at which to start the search
	 * @param fname the name to look up or create, such as "abc.def.ghi"
	 * @return the table for that name, possible a new one, or null if a non-table has that name already.
	 */
	private static LuaValue findtable(LuaState state, LuaValue table, LuaString fname) {
		int b, e = (-1);
		do {
			e = fname.indexOf(_DOT, b = e + 1);
			if (e < 0) {
				e = fname.m_length;
			}
			LuaString key = fname.substring(b, e);
			LuaValue val = table.rawget(key);
			if (val.isNil()) { /* no such field? */
				LuaTable field = new LuaTable(); /* new table for field */
				table.set(state, key, field);
				table = field;
			} else if (!val.isTable()) {  /* field has a non-table value? */
				return null;
			} else {
				table = val;
			}
		} while (e < fname.m_length);
		return table;
	}

	private static void modinit(LuaState state, LuaValue module, LuaString modname) {
		/* module._M = module */
		module.set(state, _M, module);
		int e = modname.lastIndexOf(_DOT);
		module.set(state, _NAME, modname);
		module.set(state, _PACKAGE, (e < 0 ? Constants.EMPTYSTRING : modname.substring(0, e + 1)));
	}

	/**
	 * require (modname)
	 * <p>
	 * Loads the given module. The function starts by looking into the package.loaded table to
	 * determine whether modname is already loaded. If it is, then require returns the value
	 * stored at package.loaded[modname]. Otherwise, it tries to find a loader for the module.
	 * <p>
	 * To find a loader, require is guided by the package.loaders array. By changing this array,
	 * we can change how require looks for a module. The following explanation is based on the
	 * default configuration for package.loaders.
	 * <p>
	 * First require queries package.preload[modname]. If it has a value, this value
	 * (which should be a function) is the loader. Otherwise require searches for a Lua loader
	 * using the path stored in package.path. If that also fails, it searches for a C loader
	 * using the path stored in package.cpath. If that also fails, it tries an all-in-one loader
	 * (see package.loaders).
	 * <p>
	 * Once a loader is found, require calls the loader with a single argument, modname.
	 * If the loader returns any value, require assigns the returned value to package.loaded[modname].
	 * If the loader returns no value and has not assigned any value to package.loaded[modname],
	 * then require assigns true to this entry. In any case, require returns the final value of
	 * package.loaded[modname].
	 * <p>
	 * If there is any error loading or running the module, or if it cannot find any loader for
	 * the module, then require signals an error.
	 *
	 * @param state The current lua state
	 * @param arg   Module name
	 * @return The loaded value
	 */
	public LuaValue require(LuaState state, LuaValue arg) {
		LuaString name = arg.checkLuaString();
		LuaValue loaded = state.loadedPackages.get(state, name);
		if (loaded.toBoolean()) {
			if (loaded == _SENTINEL) {
				throw new LuaError("loop or previous error loading module '" + name + "'");
			}
			return loaded;
		}

		/* else must load it; iterate over available loaders */
		LuaTable tbl = PACKAGE.get(state, _LOADERS).checkTable();
		StringBuilder sb = new StringBuilder();
		LuaValue chunk;
		for (int i = 1; true; i++) {
			LuaValue loader = tbl.get(state, i);
			if (loader.isNil()) {
				throw new LuaError("module '" + name + "' not found: " + name + sb);
			}

		    /* call loader with module name as argument */
			chunk = loader.call(state, name);
			if (chunk.isFunction()) {
				break;
			}
			if (chunk.isString()) {
				sb.append(chunk.toString());
			}
		}

		// load the module using the loader
		state.loadedPackages.set(state, name, _SENTINEL);
		LuaValue result = chunk.call(state, name);
		if (!result.isNil()) {
			state.loadedPackages.set(state, name, result);
		} else if ((result = state.loadedPackages.get(state, name)) == _SENTINEL) {
			state.loadedPackages.set(state, name, result = Constants.TRUE);
		}
		return result;
	}

	public static Varargs loadlib(Varargs args) {
		args.arg(1).checkLuaString();
		return varargsOf(Constants.NIL, ValueFactory.valueOf("dynamic libraries not enabled"), ValueFactory.valueOf("absent"));
	}

	LuaValue loader_preload(LuaState state, Varargs args) {
		LuaString name = args.arg(1).checkLuaString();
		LuaValue preload = PACKAGE.get(state, _PRELOAD).checkTable();
		LuaValue val = preload.get(state, name);
		return val.isNil() ?
			ValueFactory.valueOf("\n\tno field package.preload['" + name + "']") :
			val;
	}

	LuaValue loader_Lua(LuaState state, Varargs args) {
		String name = args.arg(1).checkString();
		InputStream is = null;


		// get package path
		LuaValue pp = PACKAGE.get(state, _PATH);
		if (!pp.isString()) {
			return ValueFactory.valueOf("package.path is not a string");
		}
		String path = pp.toString();

		// check the path elements
		int e = -1;
		int n = path.length();
		StringBuffer sb = null;
		name = name.replace('.', '/');
		while (e < n) {

			// find next template
			int b = e + 1;
			e = path.indexOf(';', b);
			if (e < 0) {
				e = path.length();
			}
			String template = path.substring(b, e);

			// create filename
			int q = template.indexOf('?');
			String filename = template;
			if (q >= 0) {
				filename = template.substring(0, q) + name + template.substring(q + 1);
			}

			// try loading the file
			Varargs v = BaseLib.loadFile(state, filename);
			if (v.first().isFunction()) {
				return v.first();
			}

			// report error
			if (sb == null) {
				sb = new StringBuffer();
			}
			sb.append("\n\t'").append(filename).append("': ").append(v.arg(2));
		}
		return ValueFactory.valueOf(sb.toString());
	}

	LuaValue loader_Java(Varargs args) {
		String name = args.arg(1).checkString();
		String classname = toClassname(name);
		Class c = null;
		LuaValue v = null;
		try {
			c = Class.forName(classname);
			v = (LuaValue) c.newInstance();
			v.setfenv(env);
			return v;
		} catch (ClassNotFoundException cnfe) {
			return ValueFactory.valueOf("\n\tno class '" + classname + "'");
		} catch (Exception e) {
			return ValueFactory.valueOf("\n\tjava load failed on '" + classname + "', " + e);
		}
	}

	/**
	 * Convert lua filename to valid class name
	 *
	 * @param filename Name of the file
	 * @return The appropriate class name
	 */
	public static String toClassname(String filename) {
		int n = filename.length();
		int j = n;
		if (filename.endsWith(".lua")) {
			j -= 4;
		}
		for (int k = 0; k < j; k++) {
			char c = filename.charAt(k);
			if ((!isClassnamePart(c)) || (c == '/') || (c == '\\')) {
				StringBuilder sb = new StringBuilder(j);
				for (int i = 0; i < j; i++) {
					c = filename.charAt(i);
					sb.append(
						(isClassnamePart(c)) ? c :
							((c == '/') || (c == '\\')) ? '.' : '_');
				}
				return sb.toString();
			}
		}
		return n == j ? filename : filename.substring(0, j);
	}

	private static boolean isClassnamePart(char c) {
		if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
			return true;
		}
		switch (c) {
			case '.':
			case '$':
			case '_':
				return true;
			default:
				return false;
		}
	}
}
