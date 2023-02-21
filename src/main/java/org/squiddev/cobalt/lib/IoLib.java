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


import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.RegisteredFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.jse.JseIoLib;
import org.squiddev.cobalt.lib.jse.JsePlatform;

import java.io.*;

import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

/**
 * Abstract base class extending {@link LibFunction} which implements the
 * core of the lua standard {@code io} library.
 * <p>
 * It contains the implementation of the io library support that is common to
 * the JSE and JME platforms.
 * In practice on of the concrete IOLib subclasses is chosen:
 * {@link JseIoLib} for the JSE platform
 * <p>
 * The JSE implementation conforms almost completely to the C-based lua library,
 * while the JME implementation follows closely except in the area of random-access files,
 * which are difficult to support properly on JME.
 * <p>
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 *
 * @see LibFunction
 * @see JsePlatform
 * @see JseIoLib
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.7">http://www.lua.org/manual/5.1/manual.html#5.7</a>
 */
public abstract class IoLib implements LuaLibrary {

	protected abstract class File extends LuaValue {
		private LuaTable metatable = fileMethods;

		protected File() {
			super(TUSERDATA);
		}

		public abstract void write(LuaString string) throws IOException;

		public abstract void flush() throws IOException;

		public abstract boolean isStandardFile();

		public abstract void close() throws IOException;

		public abstract boolean isClosed();

		// returns new position
		public abstract int seek(String option, int bytecount) throws IOException;

		public abstract void setvbuf(String mode, int size);

		// get length remaining to read
		public abstract int remaining() throws IOException;

		// peek ahead one character
		public abstract int peek() throws IOException;

		// return char if read, -1 if eof, throw IOException on other exception
		public abstract int read() throws IOException;

		// return number of bytes read if positive, false if eof, throw IOException on other exception
		public abstract int read(byte[] bytes, int offset, int length) throws IOException;

		@Override
		public LuaTable getMetatable(LuaState state) {
			return metatable;
		}

		@Override
		public void setMetatable(LuaState state, LuaTable metatable) {
			this.metatable = metatable;
		}

		// displays as "file" type
		@Override
		public String toString() {
			return "file (" + (isClosed() ? "closed" : Integer.toHexString(hashCode())) + ")";
		}
	}


	/**
	 * Wrap the standard input.
	 *
	 * @param stream The stream to wrap
	 * @return File
	 * @throws IOException On stream exception
	 */
	protected abstract File wrapStandardStream(InputStream stream) throws IOException;

	/**
	 * Wrap the standard output.
	 *
	 * @param stream The stream to wrap
	 * @return File
	 * @throws IOException On stream exception
	 */
	protected abstract File wrapStandardStream(OutputStream stream) throws IOException;

	/**
	 * Open a file in a particular mode.
	 *
	 * @param filename   Filename to open
	 * @param readMode   true if opening in read mode
	 * @param appendMode true if opening in append mode
	 * @param updateMode true if opening in update mode
	 * @param binaryMode true if opening in binary mode
	 * @return File object if successful
	 * @throws IOException if could not be opened
	 */
	protected abstract File openFile(String filename, boolean readMode, boolean appendMode, boolean updateMode, boolean binaryMode) throws IOException;

	/**
	 * Open a temporary file.
	 *
	 * @return File object if successful
	 * @throws IOException if could not be opened
	 */
	protected abstract File tmpFile() throws IOException;

	/**
	 * Start a new process and return a file for input or output
	 *
	 * @param prog the program to execute
	 * @param mode "r" to read, "w" to write
	 * @return File to read to or write from
	 * @throws IOException if an i/o exception occurs
	 */
	protected abstract File openProgram(String prog, String mode) throws IOException;

	private File inFile = null;
	private File outFile = null;
	private File errFile = null;

	private static final LuaValue STDIN = valueOf("stdin");
	private static final LuaValue STDOUT = valueOf("stdout");
	private static final LuaValue STDERR = valueOf("stderr");
	private static final LuaValue FILE = valueOf("file");
	private static final LuaValue CLOSED_FILE = valueOf("closed file");

	private LuaTable fileMethods;

	public IoLib() {
	}

	@Override
	public LuaValue add(LuaState state, LuaTable env) {

		// io lib functions
		LuaTable t = RegisteredFunction.bind(new RegisteredFunction[]{
			RegisteredFunction.ofV("close", this::close),
			RegisteredFunction.ofV("tmpfile", this::tmpfile),
			RegisteredFunction.ofV("flush", this::flush),
			RegisteredFunction.ofV("input", this::input),
			RegisteredFunction.ofV("output", this::output),
			RegisteredFunction.of("type", IoLib::type),
			RegisteredFunction.ofV("popen", this::popen),
			RegisteredFunction.ofV("open", this::open),
			RegisteredFunction.ofV("lines", this::lines),
			RegisteredFunction.ofV("read", this::read),
			RegisteredFunction.ofV("write", this::write),
		});

		// Setup streams
		try {
			t.rawset(STDIN, getCurrentInput(state));
			t.rawset(STDOUT, getCurrentIn(state));
			t.rawset(STDERR, getCurrentErr(state));
		} catch (LuaError e) {
			throw new IllegalStateException(e);
		}

		// Create file methods table
		fileMethods = RegisteredFunction.bind(new RegisteredFunction[]{
			RegisteredFunction.ofV("close", IoLib::fileClose),
			RegisteredFunction.ofV("flush", IoLib::fileFlush),
			RegisteredFunction.ofV("lines", IoLib::fileLines),
			RegisteredFunction.ofV("read", IoLib::fileRead),
			RegisteredFunction.ofV("seek", IoLib::fileSeek),
			RegisteredFunction.ofV("setvbuf", IoLib::fileSetvbuf),
			RegisteredFunction.ofV("write", IoLib::fileWrite),
		});
		fileMethods.rawset("__index", fileMethods);

		// return the table
		env.rawset("io", t);
		state.loadedPackages.rawset("io", t);
		return t;
	}

	private File getCurrentInput(LuaState state) throws LuaError {
		return inFile != null ? inFile : (inFile = doOpenFile(state, "-", "r"));
	}

	private File getCurrentIn(LuaState state) throws LuaError {
		return outFile != null ? outFile : (outFile = doOpenFile(state, "-", "w"));
	}

	private File getCurrentErr(LuaState state) throws LuaError {
		return errFile != null ? errFile : (errFile = doOpenFile(state, "-", "w"));
	}

	//	io.flush() -> bool
	private Varargs flush(LuaState state, Varargs varargs) throws LuaError {
		try {
			checkOpen(getCurrentIn(state));
			outFile.flush();
			return successResult();
		} catch (IOException e) {
			return errorResult(e);
		}
	}

	//	io.tmpfile() -> file
	private Varargs tmpfile(LuaState state, Varargs varargs) throws LuaError {
		try {
			return tmpFile();
		} catch (IOException e) {
			return errorResult(e);
		}
	}

	//	io.close([file]) -> void
	private Varargs close(LuaState state, Varargs args) throws LuaError {
		try {
			LuaValue file = args.first();
			File f = file.isNil() ? getCurrentIn(state) : checkFile(file);
			checkOpen(f);
			return doClose(f);
		} catch (IOException e) {
			return errorResult(e);
		}
	}

	//	io.input([file]) -> file
	private Varargs input(LuaState state, Varargs args) throws LuaError {
		LuaValue file = args.first();
		if (file.isNil()) {
			return getCurrentInput(state);
		} else {
			return inFile = file.isString() ? doOpenFile(state, file.checkString(), "r") : checkFile(file);
		}
	}

	// io.output(filename) -> file
	private Varargs output(LuaState state, Varargs args) throws LuaError {
		LuaValue filename = args.first();
		if (filename.isNil()) {
			return getCurrentIn(state);
		} else {
			return outFile = filename.isString() ? doOpenFile(state, filename.checkString(), "w") : checkFile(filename);
		}
	}

	//	io.type(obj) -> "file" | "closed file" | nil
	private static LuaValue type(LuaState state, LuaValue obj) {
		File f = optfile(obj);
		return f != null
			? f.isClosed() ? CLOSED_FILE : FILE
			: NIL;
	}

	// io.popen(prog, [mode]) -> file
	private Varargs popen(LuaState state, Varargs args) throws LuaError {
		String prog = args.arg(1).checkString();
		String mode = args.arg(2).optString("r");
		try {
			return openProgram(prog, mode);
		} catch (IOException e) {
			return errorResult(e);
		}
	}

	//	io.open(filename, [mode]) -> file | nil,err
	private Varargs open(LuaState state, Varargs args) throws LuaError {
		String filename = args.arg(1).checkString();
		String mode = args.arg(2).optString("r");
		try {
			return rawOpenFile(state, filename, mode);
		} catch (IOException e) {
			return errorResult(e);
		}
	}

	//	io.lines(filename) -> iterator
	private Varargs lines(LuaState state, Varargs args) throws LuaError {
		String filename = args.arg(1).optString(null);
		if (filename == null) {
			File file = getCurrentInput(state);
			checkOpen(file);
			return doLines(file, false);
		} else {
			File file = doOpenFile(state, filename, "r");
			checkOpen(file);
			return doLines(file, true);
		}
	}

	//	io.read(...) -> (...)
	private Varargs read(LuaState state, Varargs args) throws LuaError {
		checkOpen(getCurrentInput(state));
		try {
			return doRead(inFile, args);
		} catch (IOException e) {
			return errorResult(e);
		}
	}

	//	io.write(...) -> void
	private Varargs write(LuaState state, Varargs args) throws LuaError {
		checkOpen(getCurrentIn(state));
		try {
			return doWrite(outFile, args);
		} catch (IOException e) {
			return errorResult(e);
		}
	}

	// file:close() -> void
	private static Varargs fileClose(LuaState state, Varargs args) throws LuaError {
		try {
			return doClose(checkFile(args.first()));
		} catch (IOException e) {
			return errorResult(e);
		}
	}

	// file:flush() -> void
	private static Varargs fileFlush(LuaState state, Varargs args) throws LuaError {
		try {
			checkFile(args.first()).flush();
			return successResult();
		} catch (IOException e) {
			return errorResult(e);
		}
	}

	// file:setvbuf(mode,[size]) -> void
	private static Varargs fileSetvbuf(LuaState state, Varargs args) throws LuaError {
		File file = checkFile(args.first());
		String mode = args.arg(2).checkString();
		int size = args.arg(3).optInteger(1024);
		file.setvbuf(mode, size);
		return TRUE;
	}

	// file:lines() -> iterator
	private static Varargs fileLines(LuaState state, Varargs args) throws LuaError {
		return doLines(checkFile(args.first()), false);
	}

	//	file:read(...) -> (...)
	private static Varargs fileRead(LuaState state, Varargs args) throws LuaError {
		try {
			return doRead(checkFile(args.first()), args.subargs(2));
		} catch (IOException e) {
			return errorResult(e);
		}
	}

	//  file:seek([whence][,offset]) -> pos | nil,error
	private static Varargs fileSeek(LuaState state, Varargs args) throws LuaError {
		File file = checkFile(args.first());
		String whence = args.arg(2).optString("cur");
		int offset = args.arg(3).optInteger(0);
		try {
			return valueOf(checkFile(file).seek(whence, offset));
		} catch (IOException e) {
			return errorResult(e);
		}
	}

	//	file:write(...) -> void
	private static Varargs fileWrite(LuaState state, Varargs args) throws LuaError {
		try {
			return doWrite(checkFile(args.first()), args.subargs(2));
		} catch (IOException e) {
			return errorResult(e);
		}
	}

	private File doOpenFile(LuaState state, String filename, String mode) throws LuaError {
		try {
			return rawOpenFile(state, filename, mode);
		} catch (IOException e) {
			throw new LuaError("io error: " + e.getMessage());
		}
	}

	private static Varargs doClose(File f) throws IOException {
		if (f.isStandardFile()) {
			return errorResult("cannot close standard file");
		} else {
			f.close();
			return successResult();
		}
	}

	private static Varargs successResult() {
		return TRUE;
	}

	private static Varargs errorResult(Exception ioe) {
		String s = ioe.getMessage();
		return errorResult("io error: " + (s != null ? s : ioe.toString()));
	}

	private static Varargs errorResult(String message) {
		return varargsOf(NIL, valueOf(message), ZERO);
	}

	private static Varargs doLines(final File f, final boolean autoClose) {
		return new VarArgFunction() {
			@Override
			public Varargs invoke(LuaState state, Varargs args) throws LuaError {
				//	lines iterator(s,var) -> var'
				checkOpen(f);

				try {
					LuaValue result = readLine(f);
					if (autoClose && result == NIL) doClose(f);
					return result;
				} catch (IOException e) {
					return errorResult(e);
				}
			}
		};
	}

	private static Varargs doWrite(File f, Varargs args) throws IOException, LuaError {
		for (int i = 1, n = args.count(); i <= n; i++) {
			f.write(args.arg(i).checkLuaString());
		}
		return TRUE;
	}

	private static Varargs doRead(File f, Varargs args) throws IOException, LuaError {
		int i, n = args.count();
		if (n == 0) {
			return readLine(f);
		}

		LuaValue[] v = new LuaValue[n];
		LuaValue ai, vi;
		LuaString fmt;
		for (i = 0; i < n; ) {
			item:
			switch ((ai = args.arg(i + 1)).type()) {
				case TNUMBER:
					vi = readBytes(f, ai.toInteger());
					break;
				case TSTRING:
					fmt = ai.checkLuaString();
					if (fmt.length >= 2 && fmt.bytes[fmt.offset] == '*') {
						switch (fmt.bytes[fmt.offset + 1]) {
							case 'n':
								vi = readNumber(f);
								break item;
							case 'l':
								vi = readLine(f);
								break item;
							case 'a':
								vi = readAll(f);
								break item;
						}
					}
					// fallthrough
				default:
					throw ErrorFactory.argError(i + 1, "(invalid format)");
			}
			if ((v[i++] = vi).isNil()) {
				break;
			}
		}
		return i == 0 ? NIL : varargsOf(v, 0, i);
	}

	private static File checkFile(LuaValue val) throws LuaError {
		File f = optfile(val);
		if (f == null) throw ErrorFactory.argError(1, "file");
		checkOpen(f);
		return f;
	}

	private static File optfile(LuaValue val) {
		return (val instanceof File) ? (File) val : null;
	}

	private static void checkOpen(File file) throws LuaError {
		if (file.isClosed()) throw new LuaError("attempt to use a closed file");
	}

	private File rawOpenFile(LuaState state, String filename, String mode) throws IOException {
		boolean isStdFile = "-".equals(filename);
		boolean isRead = mode.startsWith("r");
		if (isStdFile) {
			return isRead ?
				wrapStandardStream(state.stdin) :
				wrapStandardStream(state.stdout);
		}
		boolean isAppend = mode.startsWith("a");
		boolean isUpdate = mode.indexOf("+") > 0;
		boolean isBinary = mode.endsWith("b");
		return openFile(filename, isRead, isAppend, isUpdate, isBinary);
	}

	// ------------- file reading utilitied ------------------

	public static LuaValue readBytes(File f, int count) throws IOException {
		byte[] b = new byte[count];
		int r;
		if ((r = f.read(b, 0, b.length)) < 0) {
			return NIL;
		}
		return LuaString.valueOf(b, 0, r);
	}

	public static LuaValue readUntil(File f, boolean lineonly) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int c;
		try {
			if (lineonly) {
				loop:
				while ((c = f.read()) >= 0) {
					switch (c) {
						case '\r':
							break;
						case '\n':
							break loop;
						default:
							baos.write(c);
							break;
					}
				}
			} else {
				while ((c = f.read()) >= 0) {
					baos.write(c);
				}
			}
		} catch (EOFException e) {
			c = -1;
		}
		return (c < 0 && baos.size() == 0) ?
			NIL :
			LuaString.valueOf(baos.toByteArray());
	}

	public static LuaValue readLine(File f) throws IOException {
		return readUntil(f, true);
	}

	public static LuaValue readAll(File f) throws IOException {
		int n = f.remaining();
		return n >= 0 ? readBytes(f, n) : readUntil(f, false);
	}

	public static LuaValue readNumber(File f) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		readChars(f, " \t\r\n", null);
		readChars(f, "-+", baos);
		//freadchars(f,"0",baos);
		//freadchars(f,"xX",baos);
		readChars(f, "0123456789", baos);
		readChars(f, ".", baos);
		readChars(f, "0123456789", baos);
		readChars(f, "eEfFgG", baos);
		readChars(f, "+-", baos);
		readChars(f, "0123456789", baos);
		String s = baos.toString();
		return s.length() > 0 ? valueOf(Double.parseDouble(s)) : NIL;
	}

	private static void readChars(File f, String chars, ByteArrayOutputStream baos) throws IOException {
		int c;
		while (true) {
			c = f.peek();
			if (chars.indexOf(c) < 0) {
				return;
			}
			f.read();
			if (baos != null) {
				baos.write(c);
			}
		}
	}


}
