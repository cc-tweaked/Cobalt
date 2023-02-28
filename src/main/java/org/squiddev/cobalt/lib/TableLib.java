/*
 * The MIT License (MIT)
 *
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2020 SquidDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.squiddev.cobalt.lib;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.function.*;
import org.squiddev.cobalt.unwind.AutoUnwind;
import org.squiddev.cobalt.unwind.SuspendedFunction;
import org.squiddev.cobalt.unwind.SuspendedTask;

import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

/**
 * Subclass of {@link LibFunction} which implements the lua standard {@code table}
 * library.
 * <p>
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 *
 * @see LibFunction
 * @see CoreLibraries
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.5">http://www.lua.org/manual/5.1/manual.html#5.5</a>
 */
public final class TableLib {
	private static final LuaValue N = LuaString.valueOf("n");

	private TableLib() {
	}

	public static void add(LuaState state, LuaTable env) {
		LuaTable t = RegisteredFunction.bind(new RegisteredFunction[]{
			RegisteredFunction.of("getn", TableLib::getn),
			RegisteredFunction.of("maxn", TableLib::maxn),
			RegisteredFunction.ofV("remove", TableLib::remove),
			RegisteredFunction.ofV("concat", TableLib::concat),
			RegisteredFunction.ofV("insert", TableLib::insert),
			RegisteredFunction.ofV("pack", TableLib::pack),
			RegisteredFunction.ofFactory("sort", Sort::new),
			RegisteredFunction.ofFactory("foreach", ForEach::new),
			RegisteredFunction.ofFactory("foreachi", ForEachI::new),
			RegisteredFunction.ofFactory("unpack", Unpack::new),
		});

		env.rawset("unpack", t.rawget("unpack"));

		LibFunction.setGlobalLibrary(state, env, "table", t);
	}

	private static LuaValue getn(LuaState state, LuaValue arg) throws LuaError {
		// getn(table) -> number
		return LuaInteger.valueOf(arg.checkTable().length());
	}

	private static LuaValue maxn(LuaState state, LuaValue arg) throws LuaError {
		// maxn(table) -> number
		return valueOf(arg.checkTable().maxn());
	}

	private static Varargs remove(LuaState state, Varargs args) throws LuaError {
		// remove (table [, pos]) -> removed-ele
		LuaTable table = args.arg(1).checkTable();
		int pos = args.count() > 1 ? args.arg(2).checkInteger() : 0;
		return table.remove(pos);
	}

	private static Varargs concat(LuaState state, Varargs args) throws LuaError {
		LuaTable table = args.arg(1).checkTable();
		return table.concat(
			args.arg(2).optLuaString(EMPTYSTRING),
			args.arg(3).optInteger(1),
			args.exists(4) ? args.arg(4).checkInteger() : table.length());
	}

	private static Varargs insert(LuaState state, Varargs args) throws LuaError {
		final LuaTable table = args.arg(1).checkTable();
		final int pos = args.count() > 2 ? args.arg(2).checkInteger() : 0;
		final LuaValue value = args.arg(args.count() > 2 ? 3 : 2);
		table.insert(pos, value);
		return NONE;
	}

	private static Varargs pack(LuaState state, Varargs args) {
		int count = args.count();
		LuaTable table = new LuaTable(count, 1);
		for (int i = 1; i <= count; i++) table.rawset(i, args.arg(i));
		table.rawset(N, valueOf(count));
		return table;
	}

	// "sort" (table [, comp]) -> void
	private static class Sort extends SuspendedVarArgFunction {
		@Override
		protected Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
			LuaTable table = args.arg(1).checkTable();
			LuaValue compare = args.isNoneOrNil(2) ? NIL : args.arg(2).checkFunction();

			SuspendedFunction<Varargs> fn = SuspendedTask.toFunction(() -> {
				// TODO: Error with "object length is not a number"
				int n = OperationHelper.length(state, table).checkInteger();
				if (n > 1) heapSort(state, table, n, compare);
				return NONE;
			});
			di.state = fn;
			return fn.call(state);
		}

		@AutoUnwind
		private static void heapSort(LuaState state, LuaTable table, int count, LuaValue compare) throws LuaError, UnwindThrowable {
			for (int start = count / 2 - 1; start >= 0; start--) {
				siftDown(state, table, start, count - 1, compare);
			}

			for (int end = count - 1; end > 0; ) {
				LuaValue endValue = OperationHelper.getTable(state, table, end + 1);
				LuaValue startValue = OperationHelper.getTable(state, table, 1);
				OperationHelper.setTable(state, table, end + 1, startValue);
				OperationHelper.setTable(state, table, 1, endValue);

				siftDown(state, table, 0, --end, compare);
			}
		}

		@AutoUnwind
		private static void siftDown(LuaState state, LuaTable table, int start, int end, LuaValue compare) throws LuaError, UnwindThrowable {
			LuaValue rootValue = OperationHelper.getTable(state, table, start + 1);

			for (int root = start; root * 2 + 1 <= end; ) {
				int child = root * 2 + 1;
				LuaValue childValue = OperationHelper.getTable(state, table, child + 1);

				if (child < end) {
					LuaValue other = OperationHelper.getTable(state, table, child + 2);
					if (compare(state, compare, childValue, other)) {
						child++;
						childValue = other;
					}
				}

				if (compare(state, compare, rootValue, childValue)) {
					OperationHelper.setTable(state, table, root + 1, childValue);
					OperationHelper.setTable(state, table, child + 1, rootValue);

					root = child; // Don't need to update rootValue, as we've now swapped!
				} else {
					return;
				}
			}
		}

		@AutoUnwind
		private static boolean compare(LuaState state, LuaValue compare, LuaValue a, LuaValue b) throws LuaError, UnwindThrowable {
			return compare.isNil()
				? OperationHelper.lt(state, a, b)
				: OperationHelper.call(state, compare, a, b).toBoolean();
		}

	}

	/**
	 * {@code foreach(table, func) -> void}: Call the supplied function once for each key-value pair
	 */
	private static class ForEach extends ResumableVarArgFunction<ForEach.State> {
		private static final class State {
			LuaValue k = NIL;
			final LuaTable table;
			final LuaValue func;

			State(LuaTable table, LuaValue func) {
				this.table = table;
				this.func = func;
			}
		}

		@Override
		protected Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
			LuaTable table = args.arg(1).checkTable();
			LuaFunction function = args.arg(2).checkFunction();

			State res = new State(table, function);
			di.state = res;
			return run(state, res);
		}

		@Override
		protected Varargs resumeThis(LuaState state, State res, Varargs value) throws LuaError, UnwindThrowable {
			return run(state, res);
		}

		private static LuaValue run(LuaState state, State res) throws LuaError, UnwindThrowable {
			Varargs n;
			LuaValue k = res.k;
			while (!(res.k = k = ((n = res.table.next(k)).first())).isNil()) {
				LuaValue r = OperationHelper.call(state, res.func, k, n.arg(2));
				if (!r.isNil()) return r;
			}
			return NIL;
		}
	}

	/**
	 * {@code foreachi(table, func) -> void}: Call the supplied function once for each key-value pair in the contiguous
	 * array part
	 */
	private static class ForEachI extends ResumableVarArgFunction<ForEachI.State> {
		private static final class State {
			int k = 0;
			final LuaTable table;
			final LuaValue func;

			State(LuaTable table, LuaValue func) {
				this.table = table;
				this.func = func;
			}
		}

		@Override
		protected Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
			LuaTable table = args.arg(1).checkTable();
			LuaFunction function = args.arg(2).checkFunction();

			State res = new State(table, function);
			di.state = res;
			return run(state, res);
		}

		@Override
		protected Varargs resumeThis(LuaState state, State res, Varargs value) throws LuaError, UnwindThrowable {
			return run(state, res);
		}

		private static LuaValue run(LuaState state, State res) throws LuaError, UnwindThrowable {
			LuaValue v;
			int k = res.k;
			while (!(v = res.table.rawget(res.k = ++k)).isNil()) {
				LuaValue r = OperationHelper.call(state, res.func, valueOf(k), v);
				if (!r.isNil()) return r;
			}
			return NIL;
		}
	}

	/**
	 * {@code unpack(table[, start[, stop]])}
	 */
	private static final class Unpack extends ResumableVarArgFunction<Unpack.State> {
		static final class State {
			final LuaValue table;
			final int start;

			int index;

			int end;
			LuaValue[] values;

			State(LuaValue table, int start) {
				this.table = table;
				this.start = start;
			}
		}

		@Override
		protected Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
			LuaValue table = args.arg(1);
			int start = args.arg(2).optInteger(1);
			State res = new State(table, start);
			di.state = res;

			LuaValue endValue = args.arg(3);
			int end = res.end = (endValue.isNil() ? OperationHelper.length(state, table) : endValue).checkInteger();
			if (start > end) return NONE;
			LuaValue[] values = res.values = new LuaValue[end - start + 1];

			for (int i = start; i <= end; res.index = ++i) {
				values[i - start] = OperationHelper.getTable(state, table, valueOf(i));
			}

			return varargsOf(values);
		}

		@Override
		protected Varargs resumeThis(LuaState state, State res, Varargs value) throws LuaError, UnwindThrowable {
			int start = res.start;
			LuaValue table = res.table;
			int end = res.end;
			LuaValue[] values = res.values;

			// If values is null, then we've yielded from fetching the length.
			if (values == null) {
				end = res.end = value.first().checkInteger();
				if (start > end) return NONE;
				values = res.values = new LuaValue[end - start + 1];
				res.index = start;
			} else {
				values[res.index - start] = value.first();
				res.index++;
			}

			for (int i = res.index; i <= end; res.index = ++i) {
				values[i - start] = OperationHelper.getTable(state, table, valueOf(i));
			}

			return varargsOf(values);
		}
	}
}
