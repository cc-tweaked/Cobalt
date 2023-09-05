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
package org.squiddev.cobalt;

/**
 * Class to encapsulate behavior of the singleton instance {@code nil}
 * <p>
 * There will be one instance of this class, {@link Constants#NIL},
 * per Java virtual machine.
 * However, the {@link Varargs} instance {@link Constants#NONE}
 * which is the empty list,
 * is also considered treated as a nil value by default.
 * <p>
 * Although it is possible to test for nil using Java == operator,
 * the recommended approach is to use the method {@link LuaValue#isNil()}
 * instead.  By using that any ambiguities between
 * {@link Constants#NIL} and {@link Constants#NONE} are avoided.
 *
 * @see LuaValue
 * @see Constants#NIL
 */
public final class LuaNil extends LuaValue {
	static final LuaNil INSTANCE = new LuaNil();

	private LuaNil() {
		super(Constants.TNIL);
	}

	@Override
	public String toString() {
		return "nil";
	}

	@Override
	public LuaTable getMetatable(LuaState state) {
		return state.nilMetatable;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof LuaNil;
	}

	@Override
	public int hashCode() {
		return 0;
	}
}
