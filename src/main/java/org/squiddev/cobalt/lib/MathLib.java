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
		t.rawset("pi", valueOf(Math.PI));
		t.rawset("huge", LuaDouble.POSINF);
		RegisteredFunction.bind(env, t, new RegisteredFunction[]{
			RegisteredFunction.of("abs", (s, arg) -> valueOf(Math.abs(arg.checkDouble()))),
			RegisteredFunction.of("ceil", (s, arg) -> valueOf(Math.ceil(arg.checkDouble()))),
			RegisteredFunction.of("cos", (s, arg) -> valueOf(Math.cos(arg.checkDouble()))),
			RegisteredFunction.of("deg", (s, arg) -> valueOf(Math.toDegrees(arg.checkDouble()))),
			RegisteredFunction.of("exp", (s, arg) -> valueOf(Math.exp(arg.checkDouble()))),
			RegisteredFunction.of("floor", (s, arg) -> valueOf(Math.floor(arg.checkDouble()))),
			RegisteredFunction.of("rad", (s, arg) -> valueOf(Math.toRadians(arg.checkDouble()))),
			RegisteredFunction.of("sin", (s, arg) -> valueOf(Math.sin(arg.checkDouble()))),
			RegisteredFunction.of("sqrt", (s, arg) -> valueOf(Math.sqrt(arg.checkDouble()))),
			RegisteredFunction.of("tan", (s, arg) -> valueOf(Math.tan(arg.checkDouble()))),
			RegisteredFunction.of("acos", (s, arg) -> valueOf(Math.acos(arg.checkDouble()))),
			RegisteredFunction.of("asin", (s, arg) -> valueOf(Math.asin(arg.checkDouble()))),
			RegisteredFunction.of("atan", (s, arg) -> valueOf(Math.atan(arg.checkDouble()))),
			RegisteredFunction.of("cosh", (s, arg) -> valueOf(Math.cosh(arg.checkDouble()))),
			RegisteredFunction.of("log10", (s, arg) -> valueOf(Math.log10(arg.checkDouble()))),
			RegisteredFunction.of("sinh", (s, arg) -> valueOf(Math.sinh(arg.checkDouble()))),
			RegisteredFunction.of("tanh", (s, arg) -> valueOf(Math.tanh(arg.checkDouble()))),
		});
		LibFunction.bind(t, MathLib2::new, new String[]{
			"fmod", "ldexp", "pow", "atan2", "log"
		});
		LibFunction.bind(t, MathLibV::new, new String[]{
			"frexp", "max", "min", "modf",
			"randomseed", "random",});
		t.rawset("mod", t.rawget("fmod"));

		env.rawset("math", t);
		state.loadedPackages.rawset("math", t);
		return t;
	}

	private static final class MathLib2 extends TwoArgFunction {
		@Override
		public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
			switch (opcode) {
				case 0: { // fmod
					double x = arg1.checkDouble();
					double y = arg2.checkDouble();
					double q = x / y;
					double f = x - y * (q >= 0 ? Math.floor(q) : Math.ceil(q));
					return valueOf(f);
				}
				case 1: { // ldexp
					double x = arg1.checkDouble();
					double y = arg2.checkDouble() + 1023.5;
					long e = (long) ((0 != (1 & ((int) y))) ? Math.floor(y) : Math.ceil(y - 1));
					return valueOf(x * Double.longBitsToDouble(e << 52));
				}
				case 2:
					return valueOf(Math.pow(arg1.checkDouble(), arg2.checkDouble()));
				case 3:
					return valueOf(Math.atan2(arg1.checkDouble(), arg2.checkDouble()));
				case 4: { // lua 5.>=2 log takes two arguments,
					if (arg2.isNil()) {
						return valueOf(Math.log(arg1.checkDouble()));
					} else {
						return valueOf(Math.log(arg1.checkDouble()) / Math.log(arg2.checkDouble()));
					}
				}
			}
			return Constants.NIL;
		}
	}

	private static final class MathLibV extends VarArgFunction {
		@Override
		public Varargs invoke(LuaState state, Varargs args) throws LuaError {
			switch (opcode) {
				case 0: { // frexp
					double x = args.arg(1).checkDouble();
					if (x == 0) return varargsOf(Constants.ZERO, Constants.ZERO);
					long bits = Double.doubleToLongBits(x);
					double m = ((bits & (~(-1L << 52))) + (1L << 52)) * ((bits >= 0) ? (.5 / (1L << 52)) : (-.5 / (1L << 52)));
					double e = (((int) (bits >> 52)) & 0x7ff) - 1022;
					return varargsOf(valueOf(m), valueOf(e));
				}
				case 1: { // max
					double m = args.arg(1).checkDouble();
					for (int i = 2, n = args.count(); i <= n; ++i) {
						m = Math.max(m, args.arg(i).checkDouble());
					}
					return valueOf(m);
				}
				case 2: { // min
					double m = args.arg(1).checkDouble();
					for (int i = 2, n = args.count(); i <= n; ++i) {
						m = Math.min(m, args.arg(i).checkDouble());
					}
					return valueOf(m);
				}
				case 3: { // modf
					double x = args.arg(1).checkDouble();
					double intPart = (x > 0) ? Math.floor(x) : Math.ceil(x);
					double fracPart = x - intPart;
					return varargsOf(valueOf(intPart), valueOf(fracPart));
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
			}
			return Constants.NONE;
		}
	}
}
