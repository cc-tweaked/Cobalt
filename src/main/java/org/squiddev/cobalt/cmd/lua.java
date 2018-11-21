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

package org.squiddev.cobalt.cmd;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.lib.jse.JsePlatform;
import org.squiddev.cobalt.lib.platform.FileResourceManipulator;
import org.squiddev.cobalt.lib.profiler.ProfilerLib;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.squiddev.cobalt.Constants.NONE;
import static org.squiddev.cobalt.ValueFactory.tableOf;
import static org.squiddev.cobalt.ValueFactory.valueOf;

/**
 * org.squiddev.cobalt.cmd.lua command for use in java se environments.
 */
public class lua {
	private static final String version = Lua._VERSION + "Copyright (c) 2009 Luaj.org.org";

	private static final String usage =
		"usage: java -cp luaj-jse.jar org.squiddev.cobalt.cmd.lua [options] [script [args]].\n" +
			"Available options are:\n" +
			"  -e stat  execute string 'stat'\n" +
			"  -l name  require library 'name'\n" +
			"  -i       enter interactive mode after executing 'script'\n" +
			"  -v       show version information\n" +
			"  -n      	nodebug - do not load debug library by default\n" +
			"  --       stop handling options\n" +
			"  -        execute stdin and stop handling options";

	private static void usageExit() {
		System.out.println(usage);
		System.exit(-1);
	}

	private static LuaTable _G;

	public static void main(String[] args) throws IOException {

		// process args
		boolean interactive = (args.length == 0);
		boolean versioninfo = false;
		boolean processing = true;
		List<String> libs = null;
		try {
			// stateful argument processing
			for (int i = 0; i < args.length; i++) {
				if (!processing || !args[i].startsWith("-")) {
					// input file - defer to last stage
					break;
				} else if (args[i].length() <= 1) {
					// input file - defer to last stage
					break;
				} else {
					switch (args[i].charAt(1)) {
						case 'e':
							if (++i >= args.length) {
								usageExit();
							}
							// input script - defer to last stage
							break;
						case 'l':
							if (++i >= args.length) {
								usageExit();
							}
							if (libs == null) libs = new ArrayList<>();
							libs.add(args[i]);
							break;
						case 'i':
							interactive = true;
							break;
						case 'v':
							versioninfo = true;
							break;
						case '-':
							if (args[i].length() > 2) {
								usageExit();
							}
							processing = false;
							break;
						default:
							usageExit();
							break;
					}
				}
			}

			// echo version
			if (versioninfo) {
				System.out.println(version);
			}

			// new org.squiddev.cobalt.cmd.lua state
			LuaState state = new LuaState();
			_G = JsePlatform.debugGlobals(state);
			_G.load(state, new ProfilerLib(new ProfilerLib.FileOutputProvider()));
			for (int i = 0, n = libs != null ? libs.size() : 0; i < n; i++) {
				loadLibrary(state, libs.get(i));
			}

			// input script processing
			processing = true;
			for (int i = 0; i < args.length; i++) {
				if (!processing || !args[i].startsWith("-")) {
					processScript(state, new FileInputStream(args[i]), args[i], args, i, false);
					break;
				} else if ("-".equals(args[i])) {
					processScript(state, System.in, "=stdin", args, i, false);
					break;
				} else {
					switch (args[i].charAt(1)) {
						case 'l':
							++i;
							break;
						case 'e':
							++i;
							processScript(state, new ByteArrayInputStream(args[i].getBytes()), "string", args, i, false);
							break;
						case '-':
							processing = false;
							break;
					}
				}
			}

			if (interactive) {
				interactiveMode(state);
			}

		} catch (IOException ioe) {
			System.err.println(ioe.toString());
			System.exit(-2);
		}
	}

	private static void loadLibrary(LuaState state, String libname) throws IOException {
		LuaValue slibname = valueOf(libname);
		try {
			// load via plain require
			OperationHelper.call(state, _G.get(state, "require"), slibname);
		} catch (Exception e) {
			try {
				// load as java class
				LuaValue v = (LuaValue) Class.forName(libname).newInstance();
				v.setfenv(_G);
				OperationHelper.call(state, v, slibname, _G);
			} catch (Exception f) {
				throw new IOException("loadLibrary(" + libname + ") failed: " + e + "," + f);
			}
		}
	}

	private static void processScript(LuaState state, InputStream script, String chunkname, String[] args, int firstarg, boolean printValue) throws IOException {
		try {
			LuaFunction c;
			try {
				c = LoadState.load(state, script, valueOf(chunkname), _G);
			} finally {
				script.close();
			}
			Varargs scriptargs = (args != null ? setGlobalArg(state, args, firstarg) : NONE);
			Varargs result = c.invoke(state, scriptargs);
			if (printValue && result != NONE) OperationHelper.invoke(state, _G.get(state, "print"), result);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	private static Varargs setGlobalArg(LuaState state, String[] args, int i) throws LuaError {
		LuaTable arg = tableOf();
		for (int j = 0; j < args.length; j++) {
			arg.set(state, j - i, valueOf(args[j]));
		}
		_G.set(state, "arg", arg);
		return OperationHelper.invoke(state, _G.get(state, "unpack"), arg);
	}

	private static void interactiveMode(LuaState state) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			System.out.print("> ");
			System.out.flush();
			String line = reader.readLine();
			if (line == null) {
				return;
			}
			processScript(state, new ByteArrayInputStream(line.getBytes()), "=stdin", null, 0, true);
		}
	}
}
