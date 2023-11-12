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
import org.squiddev.cobalt.LuaTable;

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
	 * @return Table of globals initialized with the standard JSE libraries
	 * @see #debugGlobals(LuaState)
	 * @see CoreLibraries
	 */
	public static LuaTable standardGlobals(LuaState state) throws LuaError {
		LuaTable globals = state.globals();
		BaseLib.add(globals);
		TableLib.add(state, globals);
		StringLib.add(state, globals);
		CoroutineLib.add(state, globals);
		MathLib.add(state, globals);
		Utf8Lib.add(state, globals);
		return globals;
	}

	/**
	 * Create standard globals including the {@link DebugLib} library.
	 *
	 * @param state The current lua state
	 * @return Table of globals initialized with the standard JSE and debug libraries
	 * @see #standardGlobals(LuaState)
	 * @see CoreLibraries
	 * @see DebugLib
	 */
	public static LuaTable debugGlobals(LuaState state) throws LuaError {
		LuaTable _G = standardGlobals(state);
		DebugLib.add(state, _G);
		return _G;
	}
}
