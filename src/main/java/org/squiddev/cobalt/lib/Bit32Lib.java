/*
 * ****************************************************************************
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2017 SquidDev
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

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.TwoArgFunction;
import org.squiddev.cobalt.function.VarArgFunction;

import static org.squiddev.cobalt.Constants.NIL;
import static org.squiddev.cobalt.Constants.ZERO;
import static org.squiddev.cobalt.ErrorFactory.argError;
import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.function.LibFunction.bind;

/**
 * Subclass of LibFunction that implements the Lua standard {@code bit32} library.
 */
public class Bit32Lib implements LuaLibrary {
	@Override
	public LuaValue add(LuaState state, LuaTable env) {
		LuaTable t = new LuaTable();
		bind(state, t, Bit32LibV.class, new String[]{"band", "bnot", "bor", "btest", "bxor", "extract", "replace"});
		bind(state, t, Bit32Lib2.class, new String[]{"arshift", "lrotate", "lshift", "rrotate", "rshift"});
		env.rawset("bit32", t);
		state.loadedPackages.rawset("bit32", t);
		return t;
	}

	public static final class Bit32LibV extends VarArgFunction {
		@Override
		public Varargs invoke(LuaState state, Varargs args) throws LuaError {
			switch (opcode) {
				case 0: // band
				{
					int result = -1;
					for (int i = 1; i <= args.count(); i++) {
						result &= args.arg(i).checkInteger();
					}
					return bitsToValue(result);
				}
				case 1: // bnot
					return bitsToValue(~args.arg(1).checkInteger());
				case 2: // bot
				{
					int result = 0;
					for (int i = 1; i <= args.count(); i++) {
						result |= args.arg(i).checkInteger();
					}
					return bitsToValue(result);
				}
				case 3: // btest
				{
					int bits = -1;
					for (int i = 1; i <= args.count(); i++) {
						bits &= args.arg(i).checkInteger();
					}
					return valueOf(bits != 0);
				}
				case 4: // bxor
				{
					int result = 0;
					for (int i = 1; i <= args.count(); i++) {
						result ^= args.arg(i).checkInteger();
					}
					return bitsToValue(result);
				}
				case 5: // extract
				{
					int field = args.arg(2).checkInteger();
					int width = args.arg(3).optInteger(1);

					if (field < 0) throw argError(2, "field cannot be negative");
					if (width <= 0) throw argError(3, "width must be postive");
					if (field + width > 32) throw new LuaError("trying to access non-existent bits");

					return bitsToValue((args.arg(1).checkInteger() >>> field) & (-1 >>> (32 - width)));
				}
				case 6: // replace
				{
					int n = args.arg(1).checkInteger();
					int v = args.arg(2).checkInteger();
					int field = args.arg(3).checkInteger();
					int width = args.arg(4).optInteger(1);

					if (field < 0) throw argError(3, "field cannot be negative");
					if (width <= 0) throw argError(4, "width must be postive");
					if (field + width > 32) throw new LuaError("trying to access non-existent bits");

					int mask = (-1 >>> (32 - width)) << field;
					n = (n & ~mask) | ((v << field) & mask);
					return bitsToValue(n);
				}
			}
			return NIL;
		}
	}

	public static final class Bit32Lib2 extends TwoArgFunction {
		public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
			switch (opcode) {
				case 0: // arshift
				{
					int x = arg1.checkInteger();
					int disp = arg2.checkInteger();
					return disp >= 0 ? bitsToValue(x >> disp) : bitsToValue(x << -disp);
				}
				case 1: // lrotate
					return rotate(arg1.checkInteger(), arg2.checkInteger());
				case 2: // lshift
					return shift(arg1.checkInteger(), arg2.checkInteger());
				case 3: // rrotate
					return rotate(arg1.checkInteger(), -arg2.checkInteger());
				case 4: // rshift
					return shift(arg1.checkInteger(), -arg2.checkInteger());
			}
			return NIL;
		}
	}

	static LuaValue rotate(int x, int disp) {
		if (disp < 0) {
			disp = -disp & 31;
			return bitsToValue((x >>> disp) | (x << (32 - disp)));
		} else {
			disp = disp & 31;
			return bitsToValue((x << disp) | (x >>> (32 - disp)));
		}
	}

	static LuaValue shift(int x, int disp) {
		if (disp >= 32 || disp <= -32) {
			return ZERO;
		} else if (disp >= 0) {
			return bitsToValue(x << disp);
		} else {
			return bitsToValue(x >>> -disp);
		}
	}

	private static LuaValue bitsToValue(int x) {
		return x < 0 ? valueOf((long) x & 0xFFFFFFFFL) : LuaInteger.valueOf(x);
	}
}
