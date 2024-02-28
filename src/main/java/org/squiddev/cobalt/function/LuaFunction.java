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
package org.squiddev.cobalt.function;

import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;

/**
 * Base class for functions implemented in Java.
 * <p>
 * Direct subclass include {@link LibFunction} which is the base class for
 * all built-in library functions coded in Java,
 * and {@link LuaInterpretedFunction}, which represents a lua closure
 * whose bytecode is interpreted when the function is invoked.
 *
 * @see LuaValue
 * @see LibFunction
 * @see LuaInterpretedFunction
 */
public abstract sealed class LuaFunction extends LuaValue permits LibFunction, LuaClosure {
	LuaFunction() {
		super(Constants.TFUNCTION);
	}

	@Override
	public final LuaFunction checkFunction() {
		return this;
	}

	@Override
	public final LuaTable getMetatable(LuaState state) {
		return state.functionMetatable;
	}

	public abstract String debugName();
}
