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
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.OneArgFunction;
import org.squiddev.cobalt.function.TwoArgFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.jse.JsePlatform;

import java.util.Random;

import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

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
		t.set(state, "pi", ValueFactory.valueOf(Math.PI));
		t.set(state, "huge", LuaDouble.POSINF);
		LibFunction.bind(state, t, MathLib1.class, new String[]{
			"abs", "ceil", "cos", "deg",
			"exp", "floor", "rad", "sin",
			"sqrt", "tan",
			"acos", "asin", "atan", "cosh",
			"exp", "log", "log10", "sinh",
			"tanh"
		});
		LibFunction.bind(state, t, MathLib2.class, new String[]{
			"fmod", "ldexp", "pow", "atan2"
		});
		LibFunction.bind(state, t, MathLibV.class, new String[]{
			"frexp", "max", "min", "modf",
			"randomseed", "random",});
		t.rawset("mod", t.rawget("fmod"));

		env.set(state, "math", t);
		state.loadedPackages.set(state, "math", t);
		return t;
	}

	private static final class MathLib1 extends OneArgFunction {
		@Override
		public LuaValue call(LuaState state, LuaValue arg) {
			switch (opcode) {
				case 0:
					return valueOf(Math.abs(arg.checkDouble()));
				case 1:
					return ValueFactory.valueOf(Math.ceil(arg.checkDouble()));
				case 2:
					return ValueFactory.valueOf(Math.cos(arg.checkDouble()));
				case 3:
					return ValueFactory.valueOf(Math.toDegrees(arg.checkDouble()));
				case 4:
					return ValueFactory.valueOf(Math.exp(arg.checkDouble()));
				case 5:
					return ValueFactory.valueOf(Math.floor(arg.checkDouble()));
				case 6:
					return ValueFactory.valueOf(Math.toRadians(arg.checkDouble()));
				case 7:
					return ValueFactory.valueOf(Math.sin(arg.checkDouble()));
				case 8:
					return ValueFactory.valueOf(Math.sqrt(arg.checkDouble()));
				case 9:
					return ValueFactory.valueOf(Math.tan(arg.checkDouble()));
				case 10:
					return ValueFactory.valueOf(Math.acos(arg.checkDouble()));
				case 11:
					return ValueFactory.valueOf(Math.asin(arg.checkDouble()));
				case 12:
					return ValueFactory.valueOf(Math.atan(arg.checkDouble()));
				case 13:
					return ValueFactory.valueOf(Math.cosh(arg.checkDouble()));
				case 14:
					return ValueFactory.valueOf(Math.exp(arg.checkDouble()));
				case 15:
					return ValueFactory.valueOf(Math.log(arg.checkDouble()));
				case 16:
					return ValueFactory.valueOf(Math.log10(arg.checkDouble()));
				case 17:
					return ValueFactory.valueOf(Math.sinh(arg.checkDouble()));
				case 18:
					return ValueFactory.valueOf(Math.tanh(arg.checkDouble()));
			}
			return Constants.NIL;
		}
	}

	private static final class MathLib2 extends TwoArgFunction {
		@Override
		public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) {
			switch (opcode) {
				case 0: { // fmod
					double x = arg1.checkDouble();
					double y = arg2.checkDouble();
					double q = x / y;
					double f = x - y * (q >= 0 ? Math.floor(q) : Math.ceil(q));
					return ValueFactory.valueOf(f);
				}
				case 1: { // ldexp
					double x = arg1.checkDouble();
					double y = arg2.checkDouble() + 1023.5;
					long e = (long) ((0 != (1 & ((int) y))) ? Math.floor(y) : Math.ceil(y - 1));
					return ValueFactory.valueOf(x * Double.longBitsToDouble(e << 52));
				}
				case 2:
					return ValueFactory.valueOf(Math.pow(arg1.checkDouble(), arg2.checkDouble()));
				case 3:
					return ValueFactory.valueOf(Math.atan2(arg1.checkDouble(), arg2.checkDouble()));
			}
			return Constants.NIL;
		}
	}

	private static final class MathLibV extends VarArgFunction {
		@Override
		public Varargs invoke(LuaState state, Varargs args) {
			switch (opcode) {
				case 0: { // frexp
					double x = args.arg(1).checkDouble();
					if (x == 0) return varargsOf(Constants.ZERO, Constants.ZERO);
					long bits = Double.doubleToLongBits(x);
					double m = ((bits & (~(-1L << 52))) + (1L << 52)) * ((bits >= 0) ? (.5 / (1L << 52)) : (-.5 / (1L << 52)));
					double e = (((int) (bits >> 52)) & 0x7ff) - 1022;
					return varargsOf(ValueFactory.valueOf(m), ValueFactory.valueOf(e));
				}
				case 1: { // max
					double m = args.arg(1).checkDouble();
					for (int i = 2, n = args.count(); i <= n; ++i) {
						m = Math.max(m, args.arg(i).checkDouble());
					}
					return ValueFactory.valueOf(m);
				}
				case 2: { // min
					double m = args.arg(1).checkDouble();
					for (int i = 2, n = args.count(); i <= n; ++i) {
						m = Math.min(m, args.arg(i).checkDouble());
					}
					return ValueFactory.valueOf(m);
				}
				case 3: { // modf
					double x = args.arg(1).checkDouble();
					double intPart = (x > 0) ? Math.floor(x) : Math.ceil(x);
					double fracPart = x - intPart;
					return varargsOf(ValueFactory.valueOf(intPart), ValueFactory.valueOf(fracPart));
				}
				case 4: { // randomseed
					long seed = args.arg(1).checkLong();
					state.random = new Random(seed);
					return Constants.NONE;
				}
				case 5: { // random
					if (state.random == null) {
						state.random = new Random();
					}

					switch (args.count()) {
						case 0:
							return ValueFactory.valueOf(state.random.nextDouble());
						case 1: {
							int m = args.arg(1).checkInteger();
							if (m < 1) {
								throw ErrorFactory.argError(1, "interval is empty");
							}
							return ValueFactory.valueOf(1 + state.random.nextInt(m));
						}
						default: {
							int m = args.arg(1).checkInteger();
							int n = args.arg(2).checkInteger();
							if (n < m) {
								throw ErrorFactory.argError(2, "interval is empty");
							}
							return ValueFactory.valueOf(m + state.random.nextInt(n + 1 - m));
						}
					}
				}
			}
			return Constants.NONE;
		}
	}
}
