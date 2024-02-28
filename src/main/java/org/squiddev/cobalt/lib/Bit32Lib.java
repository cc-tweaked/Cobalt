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
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.RegisteredFunction;

import static org.squiddev.cobalt.ErrorFactory.argError;
import static org.squiddev.cobalt.ValueFactory.valueOf;

/**
 * Subclass of LibFunction that implements the Lua standard {@code bit32} library.
 */
public final class Bit32Lib {
	private Bit32Lib() {
	}

	public static void add(LuaState state, LuaTable env) throws LuaError {
		LibFunction.setGlobalLibrary(state, env, "bit32", RegisteredFunction.bind(new RegisteredFunction[]{
			RegisteredFunction.ofV("band", Bit32Lib::band),
			RegisteredFunction.of("bnot", Bit32Lib::bnot),
			RegisteredFunction.ofV("bor", Bit32Lib::bor),
			RegisteredFunction.ofV("btest", Bit32Lib::btest),
			RegisteredFunction.ofV("bxor", Bit32Lib::bxor),
			RegisteredFunction.of("extract", Bit32Lib::extract),
			RegisteredFunction.ofV("replace", Bit32Lib::replace),
			RegisteredFunction.of("arshift", Bit32Lib::arshift),
			RegisteredFunction.of("lrotate", Bit32Lib::lrotate),
			RegisteredFunction.of("lshift", Bit32Lib::lshift),
			RegisteredFunction.of("rrotate", Bit32Lib::rrotate),
			RegisteredFunction.of("rshift", Bit32Lib::rshift),
		}));
	}

	private static LuaValue band(LuaState state, Varargs args) throws LuaError {
		int result = -1;
		for (int i = 1; i <= args.count(); i++) {
			result &= args.arg(i).checkInteger();
		}
		return bitsToValue(result);
	}

	private static LuaValue bnot(LuaState state, LuaValue arg) throws LuaError {
		return bitsToValue(~arg.checkInteger());
	}

	private static LuaValue bor(LuaState state, Varargs args) throws LuaError {
		int result = 0;
		for (int i = 1; i <= args.count(); i++) {
			result |= args.arg(i).checkInteger();
		}
		return bitsToValue(result);
	}

	private static LuaValue btest(LuaState state, Varargs args) throws LuaError {
		int bits = -1;
		for (int i = 1; i <= args.count(); i++) {
			bits &= args.arg(i).checkInteger();
		}
		return valueOf(bits != 0);
	}

	private static LuaValue bxor(LuaState state, Varargs args) throws LuaError {
		int result = 0;
		for (int i = 1; i <= args.count(); i++) {
			result ^= args.arg(i).checkInteger();
		}
		return bitsToValue(result);
	}

	private static LuaValue extract(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3) throws LuaError {
		int field = arg2.checkInteger();
		int width = arg3.optInteger(1);

		if (field < 0) throw argError(2, "field cannot be negative");
		if (width <= 0) throw argError(3, "width must be postive");
		if (field + width > 32) throw new LuaError("trying to access non-existent bits");

		return bitsToValue((arg1.checkInteger() >>> field) & (-1 >>> (32 - width)));
	}

	private static LuaValue replace(LuaState state, Varargs args) throws LuaError {
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

	private static LuaValue arshift(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
		int x = arg1.checkInteger();
		int disp = arg2.checkInteger();
		return bitsToValue(disp >= 0 ? x >> disp : x << -disp);
	}

	private static LuaValue lrotate(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
		return bitsToValue(rotate(arg1.checkInteger(), arg2.checkInteger()));
	}

	private static LuaValue rrotate(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
		return bitsToValue(rotate(arg1.checkInteger(), -arg2.checkInteger()));
	}

	private static LuaValue lshift(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
		return bitsToValue(shift(arg1.checkInteger(), arg2.checkInteger()));
	}

	private static LuaValue rshift(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
		return bitsToValue(shift(arg1.checkInteger(), -arg2.checkInteger()));
	}

	private static int rotate(int x, int disp) {
		if (disp < 0) {
			disp = -disp & 31;
			return (x >>> disp) | (x << (32 - disp));
		} else {
			disp = disp & 31;
			return (x << disp) | (x >>> (32 - disp));
		}
	}

	private static int shift(int x, int disp) {
		if (disp >= 32 || disp <= -32) {
			return 0;
		} else if (disp >= 0) {
			return x << disp;
		} else {
			return x >>> -disp;
		}
	}

	private static LuaValue bitsToValue(int x) {
		return x < 0 ? valueOf((long) x & 0xFFFFFFFFL) : LuaInteger.valueOf(x);
	}
}
