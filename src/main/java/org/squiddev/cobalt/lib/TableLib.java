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
import org.squiddev.cobalt.lib.jse.JsePlatform;

import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

/**
 * Subclass of {@link LibFunction} which implements the lua standard {@code table}
 * library.
 *
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 *
 * @see LibFunction
 * @see JsePlatform
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.5">http://www.lua.org/manual/5.1/manual.html#5.5</a>
 */
public class TableLib implements LuaLibrary {
	@Override
	public LuaTable add(LuaState state, LuaTable env) {
		LuaTable t = new LuaTable();
		LibFunction.bind(t, TableLib1::new, new String[]{"getn", "maxn",});
		LibFunction.bind(t, TableLibV::new, new String[]{"remove", "concat", "insert", "pack"});
		LibFunction.bind(t, TableLibR::new, new String[]{"sort", "foreach", "foreachi", "unpack"});
		env.rawset("table", t);
		state.loadedPackages.rawset("table", t);
		return t;
	}

	private static final class TableLib1 extends OneArgFunction {

		@Override
		public LuaValue call(LuaState state, LuaValue arg) throws LuaError {
			switch (opcode) {
				case 0:  // "getn" (table) -> number
					return arg.checkTable().getn();
				case 1: // "maxn"  (table) -> number
					return valueOf(arg.checkTable().maxn());
			}
			return NIL;
		}
	}

	private static final class TableLibV extends VarArgFunction {
		@Override
		public Varargs invoke(LuaState state, Varargs args) throws LuaError {
			switch (opcode) {
				case 0: { // "remove" (table [, pos]) -> removed-ele
					LuaTable table = args.arg(1).checkTable();
					int pos = args.count() > 1 ? args.arg(2).checkInteger() : 0;
					return table.remove(pos);
				}
				case 1: { // "concat" (table [, sep [, i [, j]]]) -> string
					LuaTable table = args.arg(1).checkTable();
					return table.concat(
						args.arg(2).optLuaString(EMPTYSTRING),
						args.arg(3).optInteger(1),
						args.exists(4) ? args.arg(4).checkInteger() : table.length());
				}
				case 2: { // "insert" (table, [pos,] value) -> prev-ele
					final LuaTable table = args.arg(1).checkTable();
					final int pos = args.count() > 2 ? args.arg(2).checkInteger() : 0;
					final LuaValue value = args.arg(args.count() > 2 ? 3 : 2);
					table.insert(pos, value);
					return NONE;
				}
				case 3: { // pack(...)
					int count = args.count();
					LuaTable table = new LuaTable(count, 1);
					for (int i = 1; i <= count; i++) table.rawset(i, args.arg(i));
					table.rawset("n", valueOf(count));
					return table;
				}
				default:
					return NONE;
			}
		}
	}

	private static final class TableLibR extends ResumableVarArgFunction<Object> {
		@Override
		protected Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
			switch (opcode) {
				case 0: { // "sort" (table [, comp]) -> void
					LuaTable table = args.arg(1).checkTable();
					LuaValue compare = args.isNoneOrNil(2) ? NIL : args.arg(2).checkFunction();
					int n = table.prepSort();
					if (n > 1) {
						SortState res = new SortState(table, n, compare);
						di.state = res;
						heapSort(state, table, n, compare, res, 0, n / 2 - 1);
					}
					return NONE;
				}
				case 1: { // "foreach" (table, func) -> void
					LuaTable table = args.arg(1).checkTable();
					LuaFunction function = args.arg(2).checkFunction();

					ForEachState res = new ForEachState(table, function);
					di.state = res;
					return foreach(state, table, function, res);
				}
				case 2: { // "foreachi" (table, func) -> void
					LuaTable table = args.arg(1).checkTable();
					LuaFunction function = args.arg(2).checkFunction();

					ForEachIState res = new ForEachIState(table, function);
					di.state = res;
					return foreachi(state, table, function, res);
				}
				case 3: { // unpack(table[, start[, stop]])
					LuaValue table = args.arg(1);
					int start = args.arg(2).optInteger(1);
					UnpackState res = new UnpackState(table, start);
					di.state = res;

					LuaValue endValue = args.arg(3);
					int end = res.end = (endValue.isNil() ? OperationHelper.length(state, table) : endValue).checkInteger();
					if (start > end) return NONE;
					LuaValue[] values = res.values = new LuaValue[end - start + 1];

					for (int i = start; i <= end; i++) {
						res.index = i;
						values[i - start] = OperationHelper.getTable(state, table, valueOf(i));
					}

					return varargsOf(values);
				}
				default:
					return NONE;
			}
		}

		@Override
		protected Varargs resumeThis(LuaState state, Object object, Varargs value) throws LuaError, UnwindThrowable {
			switch (opcode) {
				case 0: { // "sort" (table [, comp]) -> void
					SortState res = (SortState) object;
					LuaTable table = res.table;
					LuaValue compare = res.compare;
					int count = res.count;

					// We attempt to recover from sifting the state
					if (res.siftState != -1) {
						int root = stateSiftDown(state, table, compare, res, value.first().toBoolean());
						res.siftState = -1;

						// Continue sifting here
						if (root != -1) normalSiftDown(state, table, root, res.end, compare, res);
					}

					// And continue sorting
					heapSort(state, table, count, compare, res, res.sortState, res.counter);
					return NONE;
				}

				case 1: { // "foreach" (table, func) -> void
					ForEachState res = (ForEachState) object;
					return foreach(state, res.table, res.func, res);
				}

				case 2: { // "foreachi" (table, func) -> void
					ForEachIState res = (ForEachIState) object;
					return foreachi(state, res.table, res.func, res);
				}

				case 3: { // unpack(table[, start[, stop]])
					UnpackState res = (UnpackState) object;
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

					for (int i = res.index; i <= end; i++) {
						res.index = i;
						values[i - start] = OperationHelper.getTable(state, table, valueOf(i));
					}

					return varargsOf(values);
				}

				default:
					throw new NonResumableException("Cannot resume " + debugName());
			}
		}
	}

	static final class ForEachState {
		public LuaValue k = NIL;
		public final LuaTable table;
		public final LuaValue func;

		ForEachState(LuaTable table, LuaValue func) {
			this.table = table;
			this.func = func;
		}
	}

	static final class UnpackState {
		private final LuaValue table;
		private final int start;
		private int index;

		private int end;
		private LuaValue[] values;

		UnpackState(LuaValue table, int start) {
			this.table = table;
			this.start = start;
		}
	}

	/**
	 * Call the supplied function once for each key-value pair
	 *
	 * @param state The current lua state
	 * @param func  The function to call
	 * @return {@link Constants#NIL}
	 */
	private static LuaValue foreach(LuaState state, LuaTable table, LuaValue func, ForEachState res) throws LuaError, UnwindThrowable {
		Varargs n;
		LuaValue k = res.k;
		while (!(res.k = k = ((n = table.next(k)).first())).isNil()) {
			LuaValue r = OperationHelper.call(state, func, k, n.arg(2));
			if (!r.isNil()) return r;
		}
		return NIL;
	}

	static final class ForEachIState {
		int k = 0;
		final LuaTable table;
		final LuaValue func;

		ForEachIState(LuaTable table, LuaValue func) {
			this.table = table;
			this.func = func;
		}
	}

	/**
	 * Call the supplied function once for each key-value pair
	 * in the contiguous array part
	 *
	 * @param state The current lua state
	 * @param func  The function to call
	 * @return {@link Constants#NIL}
	 */
	private static LuaValue foreachi(LuaState state, LuaTable table, LuaValue func, ForEachIState res) throws LuaError, UnwindThrowable {
		LuaValue v;
		int k = res.k;
		while (!(v = table.rawget(res.k = ++k)).isNil()) {
			LuaValue r = OperationHelper.call(state, func, valueOf(k), v);
			if (!r.isNil()) return r;
		}
		return NIL;
	}

	private static final class SortState {
		final LuaTable table;
		final int count;
		final LuaValue compare;

		// Top level state
		int sortState = 0;
		int counter = -1;

		int siftState = -1;
		int root;
		int child;
		int end;

		private SortState(LuaTable table, int count, LuaValue compare) {
			this.table = table;
			this.count = count;
			this.compare = compare;
		}
	}

	private static void heapSort(LuaState state, LuaTable table, int count, LuaValue compare, SortState res, int sortState, int counter) throws LuaError, UnwindThrowable {
		switch (sortState) {
			case 0: {
				int start = counter;
				try {
					for (; start >= 0; --start) {
						normalSiftDown(state, table, start, count - 1, compare, res);
					}
				} catch (UnwindThrowable e) {
					res.sortState = 0;
					res.counter = start - 1;
					throw e;
				}

				// Allow explicit fall-through into the next state
				// Therefore we want to reset all the various counters.
				counter = count - 1;
			}

			case 1: {
				int end = counter;
				try {
					for (; end > 0; ) {
						table.swap(end, 0);
						normalSiftDown(state, table, 0, --end, compare, res);
					}
				} catch (UnwindThrowable e) {
					res.sortState = 1;
					res.counter = end;
					throw e;
				}
			}
		}
	}

	private static void normalSiftDown(LuaState state, LuaTable table, int root, int end, LuaValue compare, SortState res) throws LuaError, UnwindThrowable {
		int siftState = -1, child = -1;
		try {
			for (; root * 2 + 1 <= end; ) {
				siftState = 0;

				child = root * 2 + 1;
				if (child < end && table.compare(state, child, child + 1, compare)) {
					++child;
				}

				siftState = 1;

				if (table.compare(state, root, child, compare)) {
					table.swap(root, child);
					root = child;
				} else {
					return;
				}
			}
		} catch (UnwindThrowable e) {
			res.root = root;
			res.child = child;
			res.siftState = siftState;
			res.end = end;
			throw e;
		}
	}

	private static int stateSiftDown(LuaState state, LuaTable table, LuaValue compare, SortState res, boolean value) throws LuaError, UnwindThrowable {
		int siftState = res.siftState;
		int child = res.child;
		int root = res.root;

		switch (siftState) {
			case 0:
				if (value) child++;

				// This is technically state 1 now, but we care about the exit
				// point rather than the entry point
				try {
					value = table.compare(state, root, child, compare);
				} catch (UnwindThrowable e) {
					res.child = child;
					res.siftState = 1;
					throw e;
				}
				// Allow fall through
			case 1:
				if (value) {
					table.swap(root, child);
					return child;
				} else {
					return -1;
				}
			default:
				throw new IllegalStateException("No such state " + siftState);
		}
	}
}
