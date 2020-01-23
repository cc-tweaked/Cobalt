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
import org.squiddev.cobalt.lib.jse.JsePlatform;

import java.util.Random;

import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;
import static org.squiddev.cobalt.function.LibFunction.*;

/**
 * Subclass of {@link LibFunction} which implements the lua standard {@code math}
 * library.
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 *
 * @see LibFunction
 * @see JsePlatform
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.6">http://www.lua.org/manual/5.1/manual.html#5.6</a>
 */
public class MathLib implements LuaLibrary {
	@Override
	public LuaValue add(LuaState state, LuaTable env) {
		LuaTable t = new LuaTable(0, 30);
		t.rawset("pi", valueOf(Math.PI));
		t.rawset("huge", LuaDouble.POSINF);

		bindV(t, "frexp", MathLib::frexp);
		bindV(t, "max", MathLib::max);
		bindV(t, "min", MathLib::min);
		bindV(t, "modf", MathLib::modf);
		bindV(t, "randomseed", MathLib::randomseed);
		bindV(t, "random", MathLib::random);

		bind1(t, "abs", (s, arg) -> valueOf(Math.abs(arg.checkDouble())));
		bind1(t, "ceil", (s, arg) -> valueOf(Math.ceil(arg.checkDouble())));
		bind1(t, "cos", (s, arg) -> valueOf(Math.cos(arg.checkDouble())));
		bind1(t, "deg", (s, arg) -> valueOf(Math.toDegrees(arg.checkDouble())));
		bind1(t, "exp", (s, arg) -> valueOf(Math.exp(arg.checkDouble())));
		bind1(t, "floor", (s, arg) -> valueOf(Math.floor(arg.checkDouble())));
		bind1(t, "rad", (s, arg) -> valueOf(Math.toRadians(arg.checkDouble())));
		bind1(t, "sin", (s, arg) -> valueOf(Math.sin(arg.checkDouble())));
		bind1(t, "sqrt", (s, arg) -> valueOf(Math.sqrt(arg.checkDouble())));
		bind1(t, "tan", (s, arg) -> valueOf(Math.tan(arg.checkDouble())));
		bind1(t, "acos", (s, arg) -> valueOf(Math.acos(arg.checkDouble())));
		bind1(t, "asin", (s, arg) -> valueOf(Math.asin(arg.checkDouble())));
		bind1(t, "atan", (s, arg) -> valueOf(Math.atan(arg.checkDouble())));
		bind1(t, "cosh", (s, arg) -> valueOf(Math.cosh(arg.checkDouble())));
		bind1(t, "exp", (s, arg) -> valueOf(Math.exp(arg.checkDouble())));
		bind1(t, "log10", (s, arg) -> valueOf(Math.log10(arg.checkDouble())));
		bind1(t, "sinh", (s, arg) -> valueOf(Math.sinh(arg.checkDouble())));
		bind1(t, "tanh", (s, arg) -> valueOf(Math.tanh(arg.checkDouble())));

		bind2(t, "fmod", MathLib::fmod);
		bind2(t, "pow", (s, x, y) -> valueOf(Math.pow(x.checkDouble(), y.checkDouble())));
		bind2(t, "ldexp", MathLib::ldexp);
		bind2(t, "atan2", (s, x, y) -> valueOf(Math.atan2(x.checkDouble(), y.checkDouble())));
		bind2(t, "log", MathLib::log);
		t.rawset("mod", t.rawget("fmod"));

		env.rawset("math", t);
		state.loadedPackages.rawset("math", t);
		return t;
	}

	static Varargs frexp(LuaState state, Varargs args) throws LuaError {
		double x = args.arg(1).checkDouble();
		if (x == 0) return varargsOf(Constants.ZERO, Constants.ZERO);
		long bits = Double.doubleToLongBits(x);
		double m = ((bits & (~(-1L << 52))) + (1L << 52)) * ((bits >= 0) ? (.5 / (1L << 52)) : (-.5 / (1L << 52)));
		double e = (((int) (bits >> 52)) & 0x7ff) - 1022;
		return varargsOf(valueOf(m), valueOf(e));
	}

	static Varargs max(LuaState state, Varargs args) throws LuaError {
		double m = args.arg(1).checkDouble();
		for (int i = 2, n = args.count(); i <= n; ++i) {
			m = Math.max(m, args.arg(i).checkDouble());
		}
		return valueOf(m);
	}

	static Varargs min(LuaState state, Varargs args) throws LuaError {
		double m = args.arg(1).checkDouble();
		for (int i = 2, n = args.count(); i <= n; ++i) {
			m = Math.min(m, args.arg(i).checkDouble());
		}
		return valueOf(m);
	}

	static Varargs modf(LuaState state, Varargs args) throws LuaError {
		double x = args.arg(1).checkDouble();
		double intPart = (x > 0) ? Math.floor(x) : Math.ceil(x);
		double fracPart = x - intPart;
		return varargsOf(valueOf(intPart), valueOf(fracPart));
	}

	static Varargs randomseed(LuaState state, Varargs args) throws LuaError {
		long seed = args.arg(1).checkLong();
		state.random = new Random(seed);
		return Constants.NONE;
	}

	static Varargs random(LuaState state, Varargs args) throws LuaError {
		if (state.random == null) {
			state.random = new Random();
		}

		switch (args.count()) {
			case 0:
				return valueOf(state.random.nextDouble());
			case 1: {
				int m = args.arg(1).checkInteger();
				if (m < 1) {
					throw ErrorFactory.argError(1, "interval is empty");
				}
				return valueOf(1 + state.random.nextInt(m));
			}
			default: {
				int m = args.arg(1).checkInteger();
				int n = args.arg(2).checkInteger();
				if (n < m) {
					throw ErrorFactory.argError(2, "interval is empty");
				}
				return valueOf(m + state.random.nextInt(n + 1 - m));
			}
		}
	}

	static LuaValue fmod(LuaState s, LuaValue arg1, LuaValue arg2) throws LuaError {
		double x = arg1.checkDouble();
		double y = arg2.checkDouble();
		double q = x / y;
		return valueOf(x - y * (q >= 0 ? Math.floor(q) : Math.ceil(q)));
	}

	static LuaValue ldexp(LuaState s, LuaValue arg1, LuaValue arg2) throws LuaError {
		double x = arg1.checkDouble();
		double y = arg2.checkDouble() + 1023.5;
		long e = (long) ((0 != (1 & ((int) y))) ? Math.floor(y) : Math.ceil(y - 1));
		return valueOf(x * Double.longBitsToDouble(e << 52));
	}

	static LuaValue log(LuaState s, LuaValue arg1, LuaValue arg2) throws LuaError {
		double arg = arg1.checkDouble();
		if (arg2.isNil()) return valueOf(Math.log(arg));

		double base = arg2.checkDouble();
		if (base == 10.0) return valueOf(Math.log10(arg));
		return valueOf(Math.log(arg) / Math.log(base));
	}
}
