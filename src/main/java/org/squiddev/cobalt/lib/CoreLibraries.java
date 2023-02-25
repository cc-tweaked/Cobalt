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

import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.lib.system.SystemLibraries;

/**
 * The {@link CoreLibraries} class is a convenience class to standardize install "core" (i.e.
 * non-{@linkplain SystemLibraries system}) into the global state.
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
	public static LuaTable standardGlobals(LuaState state) {
		LuaTable globals = state.getMainThread().getfenv();
		new BaseLib().add(globals);
		TableLib.add(state, globals);
		StringLib.add(state, globals);
		CoroutineLib.add(state, globals);
		new MathLib().add(state, globals);
		new Utf8Lib().add(state, globals);
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
	public static LuaTable debugGlobals(LuaState state) {
		LuaTable _G = standardGlobals(state);
		DebugLib.add(state, _G);
		return _G;
	}
}
