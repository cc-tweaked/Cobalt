/*
 * ****************************************************************************
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2016 SquidDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
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

import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.OneArgFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.jse.JsePlatform;

import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.ValueFactory.valueOf;

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
		LibFunction.bind(state, t, TableLib1.class, new String[]{"getn", "maxn",});
		LibFunction.bind(state, t, TableLibV.class, new String[]{
			"remove", "concat", "insert", "sort", "foreach", "foreachi",});
		env.set(state, "table", t);
		state.loadedPackages.set(state, "table", t);
		return t;
	}

	private static final class TableLib1 extends OneArgFunction {

		@Override
		public LuaValue call(LuaState state, LuaValue arg) {
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
		public Varargs invoke(LuaState state, Varargs args) {
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
				case 3: { // "sort" (table [, comp]) -> void
					LuaTable table = args.arg(1).checkTable();
					LuaValue compare = (args.isNoneOrNil(2) ? NIL : args.arg(2).checkFunction());
					table.sort(state, compare);
					return NONE;
				}
				case 4: { // (table, func) -> void
					return args.arg(1).checkTable().foreach(state, args.arg(2).checkFunction());
				}
				case 5: { // "foreachi" (table, func) -> void
					return args.arg(1).checkTable().foreachi(state, args.arg(2).checkFunction());
				}
			}
			return NONE;
		}
	}
}
