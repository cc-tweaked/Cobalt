package org.squiddev.cobalt;

import org.squiddev.cobalt.function.RegisteredFunction;
import org.squiddev.cobalt.table.TableOperations;

import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

public final class TestLib {
	private TestLib() {
	}

	public static void add(LuaTable env) {
		var t = RegisteredFunction.bind(new RegisteredFunction[]{
			RegisteredFunction.ofV("querytab", TestLib::tableQuery),
		});
		env.rawset("T", t);
	}

	private static Varargs tableQuery(LuaState state, Varargs args) throws LuaError {
		LuaTable table = args.arg(1).checkTable();
		int i = args.arg(2).optInteger(-1);

		if (i == -1) {
			return varargsOf(
				valueOf(TableOperations.getArrayLength(table)),
				valueOf(TableOperations.getHashLength(table)),
				valueOf(TableOperations.getLastFree(table))
			);
		} else if (i < TableOperations.getArrayLength(table)) {
			return varargsOf(valueOf(i), table.rawget(i), Constants.NIL);
		} else {
			throw new UnsupportedOperationException("No support for inspecting table nodes");
		}
	}
}
