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

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;

/**
 * Set up an environment with all core/safe globals installed.
 */
public final class CoreLibraries {
	private CoreLibraries() {
	}

	/**
	 * Create a standard set of globals and setup a thread
	 *
	 * @param state The current lua state
	 * @see #debugGlobals(LuaState)
	 * @see CoreLibraries
	 */
	public static void standardGlobals(LuaState state) throws LuaError {
		BaseLib.add(state);
		TableLib.add(state);
		StringLib.add(state);
		CoroutineLib.add(state);
		MathLib.add(state);
		Utf8Lib.add(state);
	}

	/**
	 * Create standard globals including the {@link DebugLib} library.
	 *
	 * @param state The current lua state
	 * @see #standardGlobals(LuaState)
	 * @see CoreLibraries
	 * @see DebugLib
	 */
	public static void debugGlobals(LuaState state) throws LuaError {
		standardGlobals(state);
		DebugLib.add(state);
	}
}
