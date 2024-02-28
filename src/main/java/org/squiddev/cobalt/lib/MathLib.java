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


import org.checkerframework.checker.nullness.qual.Nullable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.RegisteredFunction;

import java.util.Random;

import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

/**
 * Subclass of {@link LibFunction} which implements the lua standard {@code math}
 * library.
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 *
 * @see LibFunction
 * @see CoreLibraries
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.6">http://www.lua.org/manual/5.1/manual.html#5.6</a>
 */
public final class MathLib {
	private @Nullable RandomState random;

	private MathLib() {
	}

	public static void add(LuaState state, LuaTable env) throws LuaError {
		var self = new MathLib();
		final RegisteredFunction[] functions = new RegisteredFunction[]{
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
			RegisteredFunction.of("fmod", MathLib::fmod),
			RegisteredFunction.of("ldexp", MathLib::ldexp),
			RegisteredFunction.of("pow", (s, x, y) -> valueOf(Math.pow(x.checkDouble(), y.checkDouble()))),
			RegisteredFunction.of("atan2", (s, x, y) -> valueOf(Math.atan2(x.checkDouble(), y.checkDouble()))),
			RegisteredFunction.of("log", MathLib::log),
			RegisteredFunction.ofV("frexp", MathLib::frexp),
			RegisteredFunction.ofV("max", MathLib::max),
			RegisteredFunction.ofV("min", MathLib::min),
			RegisteredFunction.ofV("modf", MathLib::modf),
			// We need to capture the current random state. This is implemented as an upvalue in PUC Lua.
			RegisteredFunction.ofV("randomseed", self::randomseed),
			RegisteredFunction.ofV("random", self::random),
		};

		LuaTable t = new LuaTable(0, functions.length + 3);
		RegisteredFunction.bind(t, functions);
		t.rawset("pi", valueOf(Math.PI));
		t.rawset("huge", LuaDouble.POSINF);
		t.rawset("mod", t.rawget("fmod"));

		LibFunction.setGlobalLibrary(state, env, "math", t);
	}

	private static LuaValue fmod(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
		double x = arg1.checkDouble();
		double y = arg2.checkDouble();
		double q = x / y;
		double f = x - y * (q >= 0 ? Math.floor(q) : Math.ceil(q));
		return valueOf(f);
	}

	private static LuaValue ldexp(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
		double x = arg1.checkDouble();
		double y = arg2.checkDouble() + 1023.5;
		long e = (long) ((0 != (1 & ((int) y))) ? Math.floor(y) : Math.ceil(y - 1));
		return valueOf(x * Double.longBitsToDouble(e << 52));
	}

	private static LuaValue log(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
		if (arg2.isNil()) {
			return valueOf(Math.log(arg1.checkDouble()));
		} else {
			return valueOf(Math.log(arg1.checkDouble()) / Math.log(arg2.checkDouble()));
		}
	}

	private static Varargs frexp(LuaState state, Varargs args) throws LuaError {
		double x = args.arg(1).checkDouble();
		if (x == 0) return varargsOf(Constants.ZERO, Constants.ZERO);
		long bits = Double.doubleToLongBits(x);
		double m = ((bits & (~(-1L << 52))) + (1L << 52)) * ((bits >= 0) ? (.5 / (1L << 52)) : (-.5 / (1L << 52)));
		double e = (((int) (bits >> 52)) & 0x7ff) - 1022;
		return varargsOf(valueOf(m), valueOf(e));
	}

	private static LuaValue max(LuaState state, Varargs args) throws LuaError {
		double m = args.arg(1).checkDouble();
		for (int i = 2, n = args.count(); i <= n; ++i) {
			m = Math.max(m, args.arg(i).checkDouble());
		}
		return valueOf(m);
	}

	private static LuaValue min(LuaState state, Varargs args) throws LuaError {
		double m = args.arg(1).checkDouble();
		for (int i = 2, n = args.count(); i <= n; ++i) {
			m = Math.min(m, args.arg(i).checkDouble());
		}
		return valueOf(m);
	}

	private static Varargs modf(LuaState state, Varargs args) throws LuaError {
		double x = args.arg(1).checkDouble();
		double intPart = (x > 0) ? Math.floor(x) : Math.ceil(x);
		double fracPart = x - intPart;
		return varargsOf(valueOf(intPart), valueOf(fracPart));
	}

	private RandomState getRandom() {
		return random != null ? random : (random = new RandomState());
	}

	private Varargs randomseed(LuaState state, Varargs args) throws LuaError {
		var random = getRandom();
		long part1, part2;
		if (args.count() == 0) {
			part1 = random.seeder.nextLong();
			part2 = random.seeder.nextLong();
		} else {
			part1 = args.arg(1).checkLong();
			part2 = args.arg(2).optLong(0);
		}

		random.seed(part1, part2);
		return varargsOf(valueOf(part1), valueOf(part2));
	}

	private LuaValue random(LuaState state, Varargs args) throws LuaError {
		if (random == null) random = new RandomState();

		switch (args.count()) {
			case 0 -> {
				return valueOf(random.nextDouble());
			}
			case 1 -> {
				int high = args.arg(1).checkInteger();
				if (high < 1) throw ErrorFactory.argError(1, "interval is empty");
				return valueOf(1 + random.nextLong(high - 1));
			}
			case 2 -> {
				long low = args.arg(1).checkLong();
				long high = args.arg(2).checkLong();
				if (high < low) throw ErrorFactory.argError(2, "interval is empty");
				return valueOf(low + random.nextLong(high - low));
			}
			default -> throw new LuaError("wrong number of arguments");
		}
	}

	private static class RandomState {
		private static final int FIGS = 53;
		private static final int SHIFT_FIGS = Long.SIZE - FIGS;
		private static final double SCALE_FIG = 0.5 / (1L << (FIGS - 1));

		final Random seeder = new Random();
		private long state0, state1, state2, state3;

		RandomState() {
			seed(seeder.nextLong(), seeder.nextLong());
		}

		long nextLong() {
			long state0 = this.state0;
			long state1 = this.state1;
			long state2 = this.state2 ^ state0;
			long state3 = this.state3 ^ state1;
			long res = Long.rotateLeft(state1 * 5, 7) * 9;
			this.state0 = state0 ^ state3;
			this.state1 = state1 ^ state2;
			this.state2 = state2 ^ (state1 << 17);
			this.state3 = Long.rotateLeft(state3, 45);
			return res;
		}

		long nextLong(long n) {
			long random = nextLong();
			// If n + 1 is a power of 2, truncate it.
			if ((n & (n + 1)) == 0) return random & n;

			// Compute next power of 2 after n, minus 1.
			long lim = (1L << (64 - Long.numberOfLeadingZeros(n))) - 1;
			assert (lim & (lim + 1)) == 0;
			assert lim > n && lim >> 1 < n;

			// Project our random number into the range 0..lim.
			random &= lim;

			// If that's not inside 0..n, then just generate another random number.
			while (Long.compareUnsigned(random, n) > 0) {
				// If not inside 0..n, then abort.
				random = nextLong() & lim;
			}

			return random;
		}

		double nextDouble() {
			double value = (nextLong() >> SHIFT_FIGS) * SCALE_FIG;
			return value < 0 ? value + 1 : value;
		}

		void seed(long part1, long part2) {
			state0 = part1;
			state1 = 0xff;
			state2 = part2;
			state3 = 0;
			for (int i = 0; i < 16; i++) nextLong();
		}
	}
}
