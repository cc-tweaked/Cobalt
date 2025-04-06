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
package org.squiddev.cobalt.compiler;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.LuaInterpretedFunction;
import org.squiddev.cobalt.lib.CoreLibraries;

import java.io.InputStream;

import static org.squiddev.cobalt.ValueFactory.valueOf;

/**
 * Class to manage loading of {@link Prototype} instances.
 * <p>
 * The {@link LoadState} class exposes one main function,
 * namely {@link #load(LuaState, InputStream, LuaString, LuaValue)},
 * to be used to load code from a particular input stream.
 * <p>
 * A simple pattern for loading and executing code is
 * <pre> {@code
 * LuaValue _G = JsePlatform.standardGlobals();
 * LoadState.load( new FileInputStream("main.lua"), "main.lua", _G ).call();
 * } </pre>
 * This should work regardless of which {@link FunctionFactory}
 * has been installed.
 * <p>
 * Prior to loading code, a compiler should be installed.
 * <p>
 * By default, when using {@link CoreLibraries} to construct globals, the {@link LuaC} compiler is installed.
 *
 * @see FunctionFactory
 * @see LuaClosure
 * @see LuaFunction
 * @see LoadState#load(LuaState, InputStream, LuaString, LuaValue)
 * @see LuaC
 */
public final class LoadState {
	/**
	 * Construct our standard Lua function.
	 */
	public interface FunctionFactory {
		/**
		 * Create a {@link LuaClosure} from a {@link Prototype} and environment table.
		 *
		 * @param prototype The function prototype
		 * @param env       The function's environment.
		 * @return The loaded function
		 */
		LuaClosure load(Prototype prototype, LuaValue env);
	}

	private LoadState() {
	}

	/**
	 * A basic {@link FunctionFactory} which loads into
	 */
	public static LuaClosure interpretedFunction(Prototype prototype, LuaValue env) {
		LuaInterpretedFunction closure = new LuaInterpretedFunction(prototype);
		closure.nilUpvalues();
		if (closure.upvalues.length > 0) closure.upvalues[0].setValue(env);
		return closure;
	}

	public static LuaClosure load(LuaState state, InputStream stream, String name, LuaValue env) throws CompileException, LuaError {
		return load(state, stream, valueOf(name), env);
	}

	/**
	 * Load lua in either binary or text form from an input stream.
	 *
	 * @param state  The current lua state
	 * @param stream InputStream to read, after having read the first byte already
	 * @param name   Name to apply to the loaded chunk
	 * @param env    Environment to load into
	 * @return {@link Prototype} that was loaded
	 * @throws IllegalArgumentException If the signature is bac
	 * @throws CompileException         If the stream cannot be loaded.
	 */
	public static LuaClosure load(LuaState state, InputStream stream, LuaString name, LuaValue env) throws CompileException, LuaError {
		return state.compiler.load(LuaC.compile(state, stream, name), env);
	}

	private static final int NAME_LENGTH = 30;
	private static final int FILE_LENGTH = NAME_LENGTH - " '...' ".length() - 1;
	private static final int STRING_LENGTH = NAME_LENGTH - " [string \"...\"] ".length() - 1;

	private static final LuaString REMAINING = valueOf("...");
	private static final LuaString STRING = valueOf("[string \"");
	private static final LuaString EMPTY_STRING = valueOf("[string \"\"]");
	private static final LuaString NEW_LINES = valueOf("\r\n");

	static LuaString getShortName(LuaString name) {
		if (name.length() == 0) return EMPTY_STRING;
		switch (name.charAt(0)) {
			case '=' -> {
				return name.substringOfEnd(1, Math.min(NAME_LENGTH, name.length()));
			}
			case '@' -> { // out = "source", or "...source"
				if (name.length() - 1 > FILE_LENGTH) {
					byte[] bytes = new byte[FILE_LENGTH + 3];
					REMAINING.copyTo(bytes, 0);
					name.copyTo(name.length() - FILE_LENGTH, bytes, REMAINING.length(), FILE_LENGTH);
					return valueOf(bytes);
				} else {
					return name.substring(1);
				}
			}
		}

		int len = name.indexOfAny(NEW_LINES);
		boolean truncate = false;

		if (len < 0) {
			len = name.length();
		} else {
			// We're not at the end of the string so we add a ...
			truncate = true;
		}

		if (len > STRING_LENGTH) {
			truncate = true;
			len = STRING_LENGTH;
		}

		byte[] out = new byte[NAME_LENGTH];
		STRING.copyTo(out, 0);
		int offset = STRING.length();
		offset = name.copyTo(0, out, offset, len);
		if (truncate) offset = REMAINING.copyTo(out, offset);

		out[offset++] = '"';
		out[offset++] = ']';

		return LuaString.valueOf(out, 0, offset);
	}

	static void checkMode(LuaString mode, String current) throws CompileException {
		if (mode != null && mode.indexOf((byte) current.charAt(0)) == -1) {
			throw new CompileException("attempt to load a " + current + " chunk (mode is " + mode + ")");
		}
	}
}
