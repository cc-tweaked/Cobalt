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
package org.squiddev.cobalt.lib.system;


import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.RegisteredFunction;
import org.squiddev.cobalt.lib.CoreLibraries;

import java.io.*;
import java.nio.file.Files;

import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

/**
 * Implements the core of the Lua standard {@code io} library.
 * <p>
 * While this has been implemented to match as closely as possible the behavior in the corresponding library in C, it
 * is mostly intended
 *
 * @see LibFunction
 * @see CoreLibraries
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.7">http://www.lua.org/manual/5.1/manual.html#5.7</a>
 */
public class IoLib {
	private static final LuaValue STDIN = valueOf("stdin");
	private static final LuaValue STDOUT = valueOf("stdout");
	private static final LuaValue STDERR = valueOf("stderr");
	private static final LuaValue FILE = valueOf("file");
	private static final LuaValue CLOSED_FILE = valueOf("closed file");

	private class LuaFile extends LuaValue {
		protected final RandomAccessFile file;
		protected final InputStream is;
		protected final OutputStream os;
		protected final boolean isStandard;
		private LuaTable metatable = fileMethods;
		private boolean closed = false;
		private boolean flush = false;

		private LuaFile(RandomAccessFile file, InputStream is, OutputStream os, boolean isStandard) {
			super(TUSERDATA);
			this.file = file;
			this.is = is != null ? is.markSupported() ? is : new BufferedInputStream(is) : null;
			this.os = os;
			this.isStandard = isStandard;
		}

		LuaFile(RandomAccessFile f) {
			this(f, null, null, false);
		}

		LuaFile(InputStream i, boolean isStandard) {
			this(null, i, null, isStandard);
		}

		LuaFile(OutputStream o, boolean isStandard) {
			this(null, null, o, isStandard);
		}

		public void write(LuaString s) throws IOException {
			if (os != null) {
				s.write(os);
			} else if (file != null) {
				s.write(file);
			} else {
				throw new IOException("not implemented");
			}

			if (flush) flush();
		}

		public void flush() throws IOException {
			if (os != null) os.flush();
		}

		public boolean isStandardFile() {
			return isStandard;
		}

		public void close() throws IOException {
			closed = true;
			if (file != null) file.close();
		}

		public boolean isClosed() {
			return closed;
		}

		public int seek(String option, int pos) throws IOException {
			if (file != null) {
				if ("set".equals(option)) {
					file.seek(pos);
				} else if ("end".equals(option)) {
					file.seek(file.length() + pos);
				} else {
					file.seek(file.getFilePointer() + pos);
				}
				return (int) file.getFilePointer();
			}
			throw new IOException("not implemented");
		}

		public void setvbuf(String mode, int size) {
			flush = "no".equals(mode);
		}

		// get length remaining to read
		public int remaining() throws IOException {
			return file != null ? (int) (file.length() - file.getFilePointer()) : -1;
		}

		// peek ahead one character
		public int peek() throws IOException {
			if (is != null) {
				is.mark(1);
				int c = is.read();
				is.reset();
				return c;
			} else if (file != null) {
				long fp = file.getFilePointer();
				int c = file.read();
				file.seek(fp);
				return c;
			}
			throw new IOException("not implemented");
		}

		// return char if read, -1 if eof, throw IOException on other exception
		public int read() throws IOException {
			if (is != null) {
				return is.read();
			} else if (file != null) {
				return file.read();
			}
			throw new IOException("not implemented");
		}

		// return number of bytes read if positive, -1 if eof, throws IOException
		public int read(byte[] bytes, int offset, int length) throws IOException {
			if (file != null) {
				return file.read(bytes, offset, length);
			} else if (is != null) {
				return is.read(bytes, offset, length);
			} else {
				throw new IOException("not implemented");
			}
		}

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

	protected LuaFile openProgram(String prog, String mode) throws IOException {
		final Process p = Runtime.getRuntime().exec(prog);
		return "w".equals(mode) ?
			new LuaFile(p.getOutputStream(), false) :
			new LuaFile(p.getInputStream(), false);
	}

	private final InputStream stdin;
	private final PrintStream stdout;
	private LuaFile inFile = null;
	private LuaFile outFile = null;
	private LuaFile errFile = null;

	private LuaTable fileMethods;

	public IoLib(InputStream stdin, PrintStream stdout) {
		this.stdin = stdin;
		this.stdout = stdout;
	}

	public void add(LuaState state, LuaTable env) throws LuaError {
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
			t.rawset(STDIN, getCurrentInput());
			t.rawset(STDOUT, getCurrentIn());
			t.rawset(STDERR, getCurrentErr());
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

		LibFunction.setGlobalLibrary(state, env, "io", t);
	}

	private LuaFile getCurrentInput() throws LuaError {
		return inFile != null ? inFile : (inFile = doOpenFile("-", "r"));
	}

	private LuaFile getCurrentIn() throws LuaError {
		return outFile != null ? outFile : (outFile = doOpenFile("-", "w"));
	}

	private LuaFile getCurrentErr() throws LuaError {
		return errFile != null ? errFile : (errFile = doOpenFile("-", "w"));
	}

	//	io.flush() -> bool
	private Varargs flush(LuaState state, Varargs varargs) throws LuaError {
		try {
			checkOpen(getCurrentIn());
			outFile.flush();
			return successResult();
		} catch (IOException e) {
			return errorResult(e);
		}
	}

	//	io.tmpfile() -> file
	private Varargs tmpfile(LuaState state, Varargs varargs) {
		try {
			File file = Files.createTempFile(null, "cobalt").toFile();
			file.deleteOnExit();
			return new LuaFile(new RandomAccessFile(file, "rw"));
		} catch (IOException e) {
			return errorResult(e);
		}
	}

	//	io.close([file]) -> void
	private Varargs close(LuaState state, Varargs args) throws LuaError {
		try {
			LuaValue file = args.first();
			LuaFile f = file.isNil() ? getCurrentIn() : checkFile(file);
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
			return getCurrentInput();
		} else {
			return inFile = file.isString() ? doOpenFile(file.checkString(), "r") : checkFile(file);
		}
	}

	// io.output(filename) -> file
	private Varargs output(LuaState state, Varargs args) throws LuaError {
		LuaValue filename = args.first();
		if (filename.isNil()) {
			return getCurrentIn();
		} else {
			return outFile = filename.isString() ? doOpenFile(filename.checkString(), "w") : checkFile(filename);
		}
	}

	//	io.type(obj) -> "file" | "closed file" | nil
	private static LuaValue type(LuaState state, LuaValue obj) {
		LuaFile f = optfile(obj);
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
			return rawOpenFile(filename, mode);
		} catch (IOException e) {
			return errorResult(e);
		}
	}

	//	io.lines(filename) -> iterator
	private Varargs lines(LuaState state, Varargs args) throws LuaError {
		String filename = args.arg(1).optString(null);
		if (filename == null) {
			LuaFile file = getCurrentInput();
			checkOpen(file);
			return doLines(file, false);
		} else {
			LuaFile file = doOpenFile(filename, "r");
			checkOpen(file);
			return doLines(file, true);
		}
	}

	//	io.read(...) -> (...)
	private Varargs read(LuaState state, Varargs args) throws LuaError {
		checkOpen(getCurrentInput());
		try {
			return doRead(inFile, args);
		} catch (IOException e) {
			return errorResult(e);
		}
	}

	//	io.write(...) -> void
	private Varargs write(LuaState state, Varargs args) throws LuaError {
		checkOpen(getCurrentIn());
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
		LuaFile file = checkFile(args.first());
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
		LuaFile file = checkFile(args.first());
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

	private LuaFile doOpenFile(String filename, String mode) throws LuaError {
		try {
			return rawOpenFile(filename, mode);
		} catch (IOException e) {
			throw new LuaError("io error: " + e.getMessage());
		}
	}

	private static Varargs doClose(LuaFile f) throws IOException {
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

	private static Varargs doLines(final LuaFile f, final boolean autoClose) {
		return LibFunction.createV((state, args) -> {
			//	lines iterator(s,var) -> var'
			checkOpen(f);

			try {
				LuaValue result = readLine(f);
				if (autoClose && result == NIL) doClose(f);
				return result;
			} catch (IOException e) {
				return errorResult(e);
			}
		});
	}

	private static Varargs doWrite(LuaFile f, Varargs args) throws IOException, LuaError {
		for (int i = 1, n = args.count(); i <= n; i++) {
			f.write(args.arg(i).checkLuaString());
		}
		return TRUE;
	}

	private static Varargs doRead(LuaFile f, Varargs args) throws IOException, LuaError {
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
					if (fmt.length() >= 2 && fmt.charAt(0) == '*') {
						switch (fmt.charAt(1)) {
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
		return i == 0 ? NIL : ValueFactory.varargsOfCopy(v, 0, i);
	}

	private static LuaFile checkFile(LuaValue val) throws LuaError {
		LuaFile f = optfile(val);
		if (f == null) throw ErrorFactory.argError(1, "file");
		checkOpen(f);
		return f;
	}

	private static LuaFile optfile(LuaValue val) {
		return (val instanceof LuaFile) ? (LuaFile) val : null;
	}

	private static void checkOpen(LuaFile file) throws LuaError {
		if (file.isClosed()) throw new LuaError("attempt to use a closed file");
	}

	private LuaFile rawOpenFile(String filename, String mode) throws IOException {
		boolean isStdFile = "-".equals(filename);
		boolean isRead = mode.startsWith("r");
		if (isStdFile) return isRead ? new LuaFile(stdin, true) : new LuaFile(stdout, true);

		boolean isAppend = mode.startsWith("a");
		boolean isUpdate = mode.indexOf("+") > 0;

		RandomAccessFile f = new RandomAccessFile(filename, isRead ? "r" : "rw");
		if (isAppend) {
			f.seek(f.length());
		} else {
			if (!isRead && !isUpdate) f.setLength(0);
		}

		return new LuaFile(f);
	}

	// ------------- file reading utilitied ------------------

	private static LuaValue readBytes(LuaFile f, int count) throws IOException {
		byte[] b = new byte[count];
		int r;
		if ((r = f.read(b, 0, b.length)) < 0) {
			return NIL;
		}
		return LuaString.valueOf(b, 0, r);
	}

	private static LuaValue readUntil(LuaFile f, boolean lineonly) throws IOException {
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
		return c < 0 && baos.size() == 0 ? NIL : LuaString.valueOf(baos.toByteArray());
	}

	public static LuaValue readLine(LuaFile f) throws IOException {
		return readUntil(f, true);
	}

	public static LuaValue readAll(LuaFile f) throws IOException {
		int n = f.remaining();
		return n >= 0 ? readBytes(f, n) : readUntil(f, false);
	}

	public static LuaValue readNumber(LuaFile f) throws IOException {
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

	private static void readChars(LuaFile f, String chars, ByteArrayOutputStream baos) throws IOException {
		while (true) {
			int c = f.peek();
			if (chars.indexOf(c) < 0) return;

			f.read();
			if (baos != null) baos.write(c);
		}
	}
}
