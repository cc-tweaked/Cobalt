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

import cc.tweaked.cobalt.internal.unwind.AutoUnwind;
import cc.tweaked.cobalt.internal.unwind.SuspendedAction;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.RegisteredFunction;

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

	private static final int TABLE_READ = 1;
	private static final int TABLE_WRITE = 1 << 1;
	private static final int TABLE_LEN = 1 << 2;

	private TableLib() {
	}

	public static void add(LuaState state, LuaTable env) throws LuaError {
		LuaTable t = RegisteredFunction.bind(new RegisteredFunction[]{
			RegisteredFunction.of("getn", TableLib::getn),
			RegisteredFunction.of("maxn", TableLib::maxn),
			RegisteredFunction.ofS("remove", TableLib::remove),
			RegisteredFunction.ofS("concat", TableLib::concat),
			RegisteredFunction.ofS("insert", TableLib::insert),
			RegisteredFunction.ofS("move", TableLib::move),
			RegisteredFunction.ofV("pack", TableLib::pack),
			RegisteredFunction.ofS("sort", Sort::sort),
			RegisteredFunction.ofS("foreach", TableLib::foreach),
			RegisteredFunction.ofS("foreachi", TableLib::foreachi),
			RegisteredFunction.ofS("unpack", TableLib::unpack),
		});

		env.rawset("unpack", t.rawget("unpack"));

		LibFunction.setGlobalLibrary(state, env, "table", t);
	}

	private static LuaValue checkTableLike(LuaState state, Varargs args, int index, int flags) throws LuaError {
		LuaValue value = args.arg(index);
		if (!(value instanceof LuaTable)) {
			LuaTable metatable = value.getMetatable(state);
			if (metatable != null
				// For each operation, check (flag => metatag!=nil).
				&& ((flags & TABLE_LEN) == 0 || !metatable.rawget(CachedMetamethod.LEN).isNil())
				&& ((flags & TABLE_READ) == 0 || metatable.rawget(CachedMetamethod.INDEX).isNil())
				&& ((flags & TABLE_WRITE) == 0 || metatable.rawget(CachedMetamethod.NEWINDEX).isNil())
			) {
				return value;
			}
		}

		return value.checkTable();
	}

	private static LuaValue getn(LuaState state, LuaValue arg) throws LuaError {
		// getn(table) -> number
		return valueOf(arg.checkTable().length());
	}

	private static LuaValue maxn(LuaState state, LuaValue arg) throws LuaError {
		// maxn(table) -> number
		LuaTable table = arg.checkTable();
		double max = 0;
		LuaValue k = NIL;
		while (true) {
			Varargs pair = table.next(k);
			if ((k = pair.first()).isNil()) break;
			if (k.type() == TNUMBER) max = Math.max(max, k.toDouble());
		}

		return valueOf(max);
	}

	private static Varargs remove(LuaState state, DebugFrame frame, Varargs args) throws LuaError, UnwindThrowable {
		// remove (table [, pos]) -> removed-ele
		LuaValue table = checkTableLike(state, args, 1, TABLE_READ | TABLE_WRITE | TABLE_LEN);

		if (table instanceof LuaTable tbl && tbl.getMetatable(state) == null) {
			// Optimised case where we can access the table directly.
			int size = tbl.length();
			int pos = args.arg(2).optInteger(size);
			if (pos > size) return NONE; // Lua 5.2 would throw an error here. Not clear what the best option is!
			LuaValue v = tbl.rawget(pos);
			tbl.move(pos + 1, pos, size - pos);
			tbl.rawset(size, NIL);
			return v;
		}

		return SuspendedAction.run(frame, () -> {
			int size = OperationHelper.intLength(state, table);
			int pos = args.arg(2).optInteger(size);
			if (pos > size) return NONE;

			LuaValue v = OperationHelper.getTable(state, table, pos);
			for (; pos < size; pos++) {
				LuaValue value = OperationHelper.getTable(state, table, pos + 1);
				OperationHelper.setTable(state, table, pos, value);
			}
			OperationHelper.setTable(state, table, pos, NIL);
			return v;
		});
	}

	/**
	 * Concatenate the contents of a table efficiently.
	 */
	private static Varargs concat(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
		return SuspendedAction.run(di, () -> {
			LuaValue table = checkTableLike(state, args, 1, TABLE_READ | TABLE_LEN);
			int length = OperationHelper.intLength(state, table);

			LuaString separator = args.arg(2).optLuaString(EMPTYSTRING);
			int start = args.arg(3).optInteger(1);
			length = args.arg(4).optInteger(length);

			return concatImpl(state, table, separator, start, length);
		});
	}

	@AutoUnwind
	private static LuaValue concatImpl(LuaState state, LuaValue table, LuaString sep, int i, int j) throws LuaError, UnwindThrowable {
		Buffer sb = new Buffer();
		if (i <= j) {
			sb.append(OperationHelper.getTable(state, table, i).checkLuaString());
			while (++i <= j) {
				sb.append(sep);
				sb.append(OperationHelper.getTable(state, table, i).checkLuaString());
			}
		}
		return sb.toLuaString();
	}

	private static Varargs insert(LuaState state, DebugFrame frame, Varargs args) throws LuaError, UnwindThrowable {
		LuaValue table = checkTableLike(state, args, 1, TABLE_READ | TABLE_WRITE | TABLE_LEN);
		switch (args.count()) {
			case 2 -> {
				LuaValue value = args.arg(2);
				if (table instanceof LuaTable tbl && table.getMetatable(state) == null) {
					// Optimised case where we can access the table directly.
					tbl.rawset(tbl.length() + 1, value);
					return NONE;
				}

				return SuspendedAction.run(frame, () -> {
					int length = OperationHelper.intLength(state, table);
					OperationHelper.setTable(state, table, length + 1, value);
					return NONE;
				});
			}
			case 3 -> {
				int position = args.arg(2).checkInteger();
				LuaValue value = args.arg(3);

				if (table instanceof LuaTable tbl && table.getMetatable(state) == null) {
					// Optimised case where we can access the table directly.
					int end = Math.max(tbl.length() + 1, position);
					tbl.move(position, position + 1, end - position);
					tbl.rawset(position, value);
					return NONE;
				}

				return SuspendedAction.run(frame, () -> {
					int end = Math.max(OperationHelper.intLength(state, table) + 1, position);

					for (int i = end; i > position; i--) {
						OperationHelper.setTable(state, table, i, OperationHelper.getTable(state, table, i - 1));
					}
					OperationHelper.setTable(state, table, position, value);
					return NONE;
				});
			}
			default -> {
				throw new LuaError("wrong number of arguments to \"insert\"");
			}
		}
	}

	private static Varargs move(LuaState state, DebugFrame frame, Varargs args) throws LuaError, UnwindThrowable {
		int from = args.arg(2).checkInteger();
		int end = args.arg(3).checkInteger();
		int to = args.arg(4).checkInteger();
		LuaValue dest = args.arg(5).optValue(args.first());
		LuaValue source = checkTableLike(state, args, 1, TABLE_READ | TABLE_WRITE | TABLE_LEN);

		if (end < from) return dest;

		int count = end - from + 1;

		// Some additional overflow sanity checks.
		if (from < 1 && end >= Integer.MAX_VALUE + from) {
			throw ErrorFactory.argError(3, "too many elements to move");
		}
		if (to > Integer.MAX_VALUE - count + 1) {
			throw ErrorFactory.argError(4, "destination wrap around");
		}

		// If we've moving within the current table and have no metamethods, go through the table directly -
		// should be more optimal.
		if (source == dest && source instanceof LuaTable table && source.getMetatable(state) == null) {
			table.move(from, to, count);
			return table;
		}

		// Otherwise do the "proper implementation.
		return SuspendedAction.run(frame, () -> {
			if (to >= from + count || to <= from || !OperationHelper.eq(state, source, dest)) {
				for (int i = 0; i < count; i++) {
					LuaValue value = OperationHelper.getTable(state, source, from + i);
					OperationHelper.setTable(state, dest, to + i, value);
				}
			} else {
				for (int i = count - 1; i >= 0; i--) {
					LuaValue value = OperationHelper.getTable(state, source, from + i);
					OperationHelper.setTable(state, dest, to + i, value);
				}
			}
			return dest;
		});
	}

	public static LuaValue pack(LuaState state, Varargs args) throws LuaError {
		int count = args.count();
		LuaTable table = new LuaTable(count, 1);
		for (int i = 1; i <= count; i++) table.rawset(i, args.arg(i));
		table.rawset(N, valueOf(count));
		return table;
	}

	// "sort" (table [, comp]) -> void
	private static class Sort {
		private static Varargs sort(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
			LuaValue table = checkTableLike(state, args, 1, TABLE_LEN | TABLE_READ | TABLE_WRITE);
			return SuspendedAction.run(di, () -> {
				int n = OperationHelper.intLength(state, table);

				LuaFunction compare = args.arg(2).optFunction(null);
				if (n > 1) heapSort(state, table, n, compare);
				return NONE;
			});
		}

		@AutoUnwind
		private static void heapSort(LuaState state, LuaValue table, int count, @Nullable LuaFunction compare) throws LuaError, UnwindThrowable {
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
		private static void siftDown(LuaState state, LuaValue table, int start, int end, @Nullable LuaFunction compare) throws LuaError, UnwindThrowable {
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
		private static boolean compare(LuaState state, @Nullable LuaFunction compare, LuaValue a, LuaValue b) throws LuaError, UnwindThrowable {
			return compare == null
				? OperationHelper.lt(state, a, b)
				: Dispatch.call(state, compare, a, b).toBoolean();
		}

	}

	/**
	 * {@code foreach(table, func) -> void}: Call the supplied function once for each key-value pair
	 */
	private static Varargs foreach(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
		LuaTable table = args.arg(1).checkTable();
		LuaFunction function = args.arg(2).checkFunction();

		return SuspendedAction.run(di, () -> {
			Varargs n;
			LuaValue k = NIL;
			while (!(k = (n = table.next(k)).first()).isNil()) {
				LuaValue r = Dispatch.call(state, function, k, n.arg(2));
				if (!r.isNil()) return r;
			}
			return NIL;
		});
	}

	/**
	 * {@code foreachi(table, func) -> void}: Call the supplied function once for each key-value pair in the contiguous
	 * array part
	 */
	private static Varargs foreachi(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
		LuaTable table = args.arg(1).checkTable();
		LuaFunction function = args.arg(2).checkFunction();

		return SuspendedAction.run(di, () -> {
			LuaValue v;
			int k = 0;
			while (!(v = table.rawget(++k)).isNil()) {
				LuaValue r = Dispatch.call(state, function, valueOf(k), v);
				if (!r.isNil()) return r;
			}
			return NIL;
		});
	}

	/**
	 * {@code unpack(table[, start[, stop]])}
	 */
	private static Varargs unpack(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
		LuaValue table = args.arg(1);
		int start = args.arg(2).optInteger(1);
		LuaValue endValue = args.arg(3);

		if (table instanceof LuaTable tbl && table.getMetatable(state) == null) {
			// Do the fast path when we're a table with no metatable. In theory we could even turn this into an
			// array copy, but probably not worth it.
			int end = endValue.isNil() ? tbl.length() : endValue.checkInteger();
			if (start > end) return NONE;

			long size = (long) end - start + 1;
			if (size < 0 || size >= Integer.MAX_VALUE) throw new LuaError("too many results to unpack");

			LuaValue[] values = new LuaValue[(int) size];
			for (int i = 0; i < size; i++) values[i] = tbl.rawget(start + i);
			return varargsOf(values);
		}

		// Exactly the same code as above, but using OperationHelper.
		return SuspendedAction.run(di, () -> {
			int end = endValue.isNil() ? OperationHelper.intLength(state, table) : endValue.checkInteger();
			if (start > end) return NONE;

			LuaValue[] values = new LuaValue[end - start + 1];
			for (int i = start; i <= end; i++) {
				LuaValue value = OperationHelper.getTable(state, table, i);
				values[i - start] = value;
			}
			return varargsOf(values);
		});
	}
}
