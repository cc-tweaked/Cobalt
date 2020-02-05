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

import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.Prototype;

/**
 * Data class to hold debug information relatign to local variables for a {@link Prototype}
 */
public final class LocalVariable {
	/**
	 * The local variable name
	 */
	public final LuaString name;

	/**
	 * The instruction offset when the variable comes into scope
	 */
	public int startpc;

	/**
	 * The instruction offset when the variable goes out of scope
	 */
	public int endpc;

	/**
	 * Construct a LocVars instance.
	 *
	 * @param name    The local variable name
	 * @param startpc The instruction offset when the variable comes into scope
	 * @param endpc   The instruction offset when the variable goes out of scope
	 */
	public LocalVariable(LuaString name, int startpc, int endpc) {
		this.name = name;
		this.startpc = startpc;
		this.endpc = endpc;
	}

	@Override
	public String toString() {
		return name + " " + startpc + "-" + endpc;
	}
}
