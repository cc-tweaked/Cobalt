/**
 * ****************************************************************************
 * Copyright (c) 2009-2011 Luaj.org. All rights reserved.
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
package org.squiddev.cobalt;

/**
 * Base class for representing numbers as lua values directly.
 * <p>
 * The main subclasses are {@link LuaInteger} which holds values that fit in a java int,
 * and {@link LuaDouble} which holds all other number values.
 *
 * @see LuaInteger
 * @see LuaDouble
 * @see LuaValue
 */
public abstract class LuaNumber extends LuaValue {

	public LuaNumber() {
		super(Constants.TNUMBER);
	}

	@Override
	public LuaNumber checkNumber() {
		return this;
	}

	@Override
	public LuaNumber checkNumber(String errmsg) {
		return this;
	}

	@Override
	public LuaNumber optNumber(LuaNumber defval) {
		return this;
	}

	@Override
	public LuaValue toNumber() {
		return this;
	}

	@Override
	public boolean isNumber() {
		return true;
	}

	@Override
	public LuaValue getMetatable(LuaState state) {
		return state.numberMetatable;
	}

	@Override
	public LuaValue concat(LuaState state, LuaValue rhs) {
		return rhs.concatTo(state, this);
	}

	@Override
	public Buffer concat(LuaState state, Buffer rhs) {
		return rhs.concatTo(state, this);
	}

	@Override
	public LuaValue concatTo(LuaState state, LuaNumber lhs) {
		return strvalue().concatTo(state, lhs.strvalue());
	}

	@Override
	public LuaValue concatTo(LuaState state, LuaString lhs) {
		return strvalue().concatTo(state, lhs);
	}

	@Override
	public double checkArith() {
		return toDouble();
	}
}
