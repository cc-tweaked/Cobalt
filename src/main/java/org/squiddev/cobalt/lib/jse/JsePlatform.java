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
package org.squiddev.cobalt.lib.jse;

import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.compiler.LuaC;
import org.squiddev.cobalt.lib.*;
import org.squiddev.cobalt.lib.platform.ResourceManipulator;

/**
 * The {@link JsePlatform} class is a convenience class to standardize
 * how globals tables are initialized for the JSE platform.
 *
 * It is used to allocate either a set of standard globals using
 * {@link #standardGlobals(LuaState)} or debug globals using {@link #debugGlobals(LuaState)}
 *
 * A simple example of initializing globals and using them from Java is:
 * <pre> {@code
 * LuaValue _G = JsePlatform.standardGlobals();
 * _G.get("print").call(LuaValue.valueOf("hello, world"));
 * } </pre>
 *
 * Once globals are created, a simple way to load and run a script is:
 * <pre> {@code
 * LoadState.load(new FileInputStream("main.lua"), "main.lua", _G ).call();
 * } </pre>
 *
 * although {@code require} could also be used:
 * <pre> {@code
 * _G.get("require").call(LuaValue.valueOf("main"));
 * } </pre>
 * For this to succeed, the file "main.lua" must be in the current directory or a resource.
 * See {@link BaseLib} for details on finding scripts using {@link ResourceManipulator}.
 *
 * The standard globals will contain all standard libraries plus {@code luajava}:
 * <ul>
 * <li>{@link BaseLib}</li>
 * <li>{@link PackageLib}</li>
 * <li>{@link TableLib}</li>
 * <li>{@link StringLib}</li>
 * <li>{@link CoroutineLib}</li>
 * <li>{@link MathLib}</li>
 * <li>{@link JseIoLib}</li>
 * <li>{@link OsLib}</li>
 * </ul>
 * In addition, the {@link LuaC} compiler is installed so lua files may be loaded in their source form.
 *
 * The debug globals are simply the standard globals plus the {@code debug} library {@link DebugLib}.
 */
public class JsePlatform {

	/**
	 * Create a standard set of globals and setup a thread
	 *
	 * @param state The current lua state
	 * @return Table of globals initialized with the standard JSE libraries
	 * @see #debugGlobals(LuaState)
	 * @see JsePlatform
	 */
	public static LuaTable standardGlobals(LuaState state) {
		LuaTable _G = new LuaTable();
		state.setupThread(_G);
		_G.load(state, new BaseLib());
		_G.load(state, new PackageLib());
		_G.load(state, new TableLib());
		_G.load(state, new StringLib());
		_G.load(state, new CoroutineLib());
		_G.load(state, new MathLib());
		_G.load(state, new JseIoLib());
		_G.load(state, new OsLib());
		return _G;
	}

	/**
	 * Create standard globals including the {@link DebugLib} library.
	 *
	 * @param state The current lua state
	 * @return Table of globals initialized with the standard JSE and debug libraries
	 * @see #standardGlobals(LuaState)
	 * @see JsePlatform
	 * @see DebugLib
	 */
	public static LuaTable debugGlobals(LuaState state) {
		LuaTable _G = standardGlobals(state);
		_G.load(state, new DebugLib());
		return _G;
	}
}
