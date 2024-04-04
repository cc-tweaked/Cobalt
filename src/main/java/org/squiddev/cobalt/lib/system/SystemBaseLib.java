package org.squiddev.cobalt.lib.system;

import cc.tweaked.cobalt.internal.unwind.SuspendedAction;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.function.RegisteredFunction;

import java.io.InputStream;
import java.io.PrintStream;

import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

/**
 * Adds additional globals to the base library that interact with the running system, and so may not be safe to use in
 * a sandboxed environment.
 */
public class SystemBaseLib {
	private static final LuaString STDIN_STR = valueOf("=stdin");

	private final ResourceLoader resources;
	private final InputStream in;
	private final PrintStream out;

	public SystemBaseLib(ResourceLoader resources, InputStream in, PrintStream out) {
		this.resources = resources;
		this.in = in;
		this.out = out;
	}

	public void add(LuaTable env) {
		RegisteredFunction.bind(env, new RegisteredFunction[]{
			RegisteredFunction.of("collectgarbage", SystemBaseLib::collectgarbage),
			RegisteredFunction.ofV("loadfile", this::loadfile),
			RegisteredFunction.ofS("dofile", this::dofile),
			RegisteredFunction.ofS("print", this::print),
		});
	}

	private static LuaValue collectgarbage(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
		// collectgarbage( opt [,arg] ) -> value
		String s = arg1.optString("collect");
		switch (s) {
			case "collect" -> {
				System.gc();
				return Constants.ZERO;
			}
			case "count" -> {
				Runtime rt = Runtime.getRuntime();
				long used = rt.totalMemory() - rt.freeMemory();
				return valueOf(used / 1024.);
			}
			case "step" -> {
				System.gc();
				return Constants.TRUE;
			}
			default -> throw ErrorFactory.argError(1, "invalid option");
		}
	}

	private Varargs loadfile(LuaState state, Varargs args) throws LuaError {
		// loadfile( [filename] ) -> chunk | nil, msg
		return args.first().isNil() ?
			SystemBaseLib.loadBasicStream(state, in, STDIN_STR) :
			SystemBaseLib.loadFile(state, resources, args.arg(1).checkString());
	}

	private Varargs dofile(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
		// dofile( filename ) -> result1, ...
		Varargs v = args.first().isNil() ?
			SystemBaseLib.loadBasicStream(state, in, STDIN_STR) :
			SystemBaseLib.loadFile(state, resources, args.arg(1).checkString());
		if (v.first().isNil()) {
			throw new LuaError(v.arg(2).toString());
		} else {
			return SuspendedAction.run(di, () -> Dispatch.invoke(state, v.first(), Constants.NONE));
		}
	}

	private Varargs print(LuaState state, DebugFrame frame, Varargs args) throws LuaError, UnwindThrowable {
		// print(...) -> void
		return SuspendedAction.run(frame, () -> {
			LuaValue tostring = OperationHelper.getTable(state, state.globals(), valueOf("tostring"));
			for (int i = 1, n = args.count(); i <= n; i++) {
				if (i > 1) out.write('\t');
				LuaValue value = Dispatch.call(state, tostring, args.arg(i));
				LuaString s = value.checkLuaString();
				int z = s.indexOf((byte) 0);

				int len = z >= 0 ? z : s.length();
				byte[] bytes = new byte[len];
				s.copyTo(0, bytes, 0, len);
				out.write(bytes, 0, len);
			}
			out.println();
			return Constants.NONE;
		});
	}

	private static Varargs loadBasicStream(LuaState state, InputStream is, LuaString chunkName) {
		try {
			return LoadState.load(state, is, chunkName, state.globals());
		} catch (LuaError | CompileException e) {
			return varargsOf(Constants.NIL, valueOf(e.getMessage()));
		}
	}

	/**
	 * Load from a named file, returning the chunk or nil,error of can't load
	 *
	 * @param state    The current lua state
	 * @param filename Name of the file
	 * @return Varargs containing chunk, or NIL,error-text on error
	 */
	public static Varargs loadFile(LuaState state, ResourceLoader resources, String filename) {
		InputStream is = resources.load(filename);
		if (is == null) {
			return varargsOf(Constants.NIL, valueOf("cannot open " + filename + ": No such file or directory"));
		}
		try {
			return loadBasicStream(state, is, valueOf("@" + filename));
		} finally {
			try {
				is.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
