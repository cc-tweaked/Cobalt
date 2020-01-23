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
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.jse.JseIoLib;
import org.squiddev.cobalt.lib.jse.JsePlatform;

import java.io.*;

import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;
import static org.squiddev.cobalt.function.LibFunction.bindV;

/**
 * Abstract base class extending {@link LibFunction} which implements the
 * core of the lua standard {@code io} library.
 *
 * It contains the implementation of the io library support that is common to
 * the JSE and JME platforms.
 * In practice on of the concrete IOLib subclasses is chosen:
 * {@link JseIoLib} for the JSE platform
 *
 * The JSE implementation conforms almost completely to the C-based lua library,
 * while the JME implementation follows closely except in the area of random-access files,
 * which are difficult to support properly on JME.
 *
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 *
 * @see LibFunction
 * @see JsePlatform
 * @see JseIoLib
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.7">http://www.lua.org/manual/5.1/manual.html#5.7</a>
 */
public abstract class IoLib implements LuaLibrary {

	protected abstract class File extends LuaValue {
		private LuaTable metatable = filemethods;

		protected File() {
			super(TUSERDATA);
		}

		public abstract void write(LuaString string) throws IOException;

		public abstract void flush() throws IOException;

		public abstract boolean isstdfile();

		public abstract void close() throws IOException;

		public abstract boolean isclosed();

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
			return "file (" + (isclosed() ? "closed" : Integer.toHexString(hashCode())) + ")";
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

	private File infile = null;
	private File outfile = null;
	private File errfile = null;

	private static final LuaValue STDIN = valueOf("stdin");
	private static final LuaValue STDOUT = valueOf("stdout");
	private static final LuaValue STDERR = valueOf("stderr");
	private static final LuaValue FILE = valueOf("file");
	private static final LuaValue CLOSED_FILE = valueOf("closed file");

	private LuaTable filemethods;

	public IoLib() {
	}

	@Override
	public LuaValue add(LuaState state, LuaTable env) {

		// io lib functions
		LuaTable t = new LuaTable();

		bindIO(t, "flush", (s, args) -> flush(s));
		bindIO(t, "tmpfile", (s, args) -> tmpfile());
		bindIO(t, "close", (s, args) -> close(s, args.first()));
		bindIO(t, "input", (s, args) -> input(s, args.first()));
		bindIO(t, "output", (s, args) -> output(s, args.first()));
		bindIO(t, "type", (s, args) -> type(args.first()));
		bindIO(t, "popen", (s, args) -> popen(args.arg(1).checkString(), args.arg(2).optString("r")));
		bindIO(t, "open", (s, args) -> open(s, args.arg(1).checkString(), args.arg(2).optString("r")));
		bindIO(t, "lines", (s, args) -> lines(s, args.exists(1) ? args.arg(1).checkString() : null));
		bindIO(t, "read", this::read);
		bindIO(t, "write", this::write);

		// setup streams
		try {
			t.rawset(STDIN, input(state));
			t.rawset(STDOUT, output(state));
			t.rawset(STDERR, errput(state));
		} catch (LuaError e) {
			throw new IllegalStateException(e);
		}

		// create file methods table
		filemethods = new LuaTable();
		bindIO(filemethods, "close", (s, args) -> fileClose(args.first()));
		bindIO(filemethods, "flush", (s, args) -> fileFlush(args.first()));
		bindIO(filemethods, "setvbuf", (s, args) -> fileSetBuf(args.first(), args.arg(2).checkString(), args.arg(3).optInteger(1024)));
		bindIO(filemethods, "lines", (s, args) -> fileLines(args.first()));
		bindIO(filemethods, "read", (s, args) -> fileRead(args.first(), args.subargs(2)));
		bindIO(filemethods, "seek", (s, args) -> fileSeek(args.first(), args.arg(2).optString("cur"), args.arg(3).optInteger(0)));
		bindIO(filemethods, "write", (s, args) -> fileWrite(args.first(), args.subargs(2)));

		// setup library and index
		filemethods.rawset("__index", filemethods);

		// return the table
		env.rawset("io", t);
		state.loadedPackages.rawset("io", t);
		return t;
	}

	interface IODelegate {
		Varargs call(LuaState state, Varargs arg) throws LuaError, IOException;
	}

	private static void bindIO(LuaTable table, String name, IODelegate delegate) {
		bindV(table, name, (s, v) -> {
			try {
				return delegate.call(s, v);
			} catch (IOException e) {
				return errorresult(e);
			}
		});
	}

	private File input(LuaState state) throws LuaError {
		return infile != null ? infile : (infile = ioopenfile(state, "-", "r"));
	}

	//	io.flush() -> bool
	public Varargs flush(LuaState state) throws IOException, LuaError {
		checkopen(output(state));
		outfile.flush();
		return TRUE;
	}

	//	io.tmpfile() -> file
	public Varargs tmpfile() throws IOException {
		return tmpFile();
	}

	//	io.close([file]) -> void
	public Varargs close(LuaState state, LuaValue file) throws IOException, LuaError {
		File f = file.isNil() ? output(state) : checkfile(file);
		checkopen(f);
		return ioclose(f);
	}

	//	io.input([file]) -> file
	public Varargs input(LuaState state, LuaValue file) throws LuaError {
		if (file.isNil()) {
			return input(state);
		} else {
			return infile = file.isString() ? ioopenfile(state, file.checkString(), "r") : checkfile(file);
		}
	}

	// io.output(filename) -> file
	public Varargs output(LuaState state, LuaValue filename) throws LuaError {
		if (filename.isNil()) {
			return output(state);
		} else {
			return outfile = filename.isString() ? ioopenfile(state, filename.checkString(), "w") : checkfile(filename);
		}
	}

	//	io.type(obj) -> "file" | "closed file" | nil
	public Varargs type(LuaValue obj) {
		File f = optfile(obj);
		return f != null ?
			f.isclosed() ? CLOSED_FILE : FILE :
			NIL;
	}

	// io.popen(prog, [mode]) -> file
	public Varargs popen(String prog, String mode) throws IOException {
		return openProgram(prog, mode);
	}

	//	io.open(filename, [mode]) -> file | nil,err
	public Varargs open(LuaState state, String filename, String mode) throws IOException {
		return rawopenfile(state, filename, mode);
	}

	//	io.lines(filename) -> iterator
	public Varargs lines(LuaState state, String filename) throws LuaError {
		if (filename == null) {
			File file = input(state);
			checkopen(file);
			return lines(file, false);
		} else {
			File file = ioopenfile(state, filename, "r");
			checkopen(file);
			return lines(file, true);
		}
	}

	//	io.read(...) -> (...)
	public Varargs read(LuaState state, Varargs args) throws IOException, LuaError {
		checkopen(input(state));
		return ioread(infile, args);
	}

	//	io.write(...) -> void
	public Varargs write(LuaState state, Varargs args) throws IOException, LuaError {
		checkopen(output(state));
		return iowrite(outfile, args);
	}

	// file:close() -> void
	public Varargs fileClose(LuaValue file) throws IOException, LuaError {
		return ioclose(checkfile(file));
	}

	// file:flush() -> void
	public Varargs fileFlush(LuaValue file) throws IOException, LuaError {
		checkfile(file).flush();
		return TRUE;
	}

	// file:setvbuf(mode,[size]) -> void
	public Varargs fileSetBuf(LuaValue file, String mode, int size) throws LuaError {
		checkfile(file).setvbuf(mode, size);
		return TRUE;
	}

	// file:lines() -> iterator
	public Varargs fileLines(LuaValue file) throws LuaError {
		return lines(checkfile(file), false);
	}

	//	file:read(...) -> (...)
	public Varargs fileRead(LuaValue file, Varargs subargs) throws IOException, LuaError {
		return ioread(checkfile(file), subargs);
	}

	//  file:seek([whence][,offset]) -> pos | nil,error
	public Varargs fileSeek(LuaValue file, String whence, int offset) throws IOException, LuaError {
		return valueOf(checkfile(file).seek(whence, offset));
	}

	//	file:write(...) -> void
	public Varargs fileWrite(LuaValue file, Varargs subargs) throws IOException, LuaError {
		return iowrite(checkfile(file), subargs);
	}

	private File output(LuaState state) throws LuaError {
		return outfile != null ? outfile : (outfile = ioopenfile(state, "-", "w"));
	}

	private File errput(LuaState state) throws LuaError {
		return errfile != null ? errfile : (errfile = ioopenfile(state, "-", "w"));
	}

	private File ioopenfile(LuaState state, String filename, String mode) throws LuaError {
		try {
			return rawopenfile(state, filename, mode);
		} catch (Exception e) {
			throw new LuaError("io error: " + e.getMessage());
		}
	}

	private static Varargs ioclose(File f) throws IOException {
		if (f.isstdfile()) {
			return errorresult("cannot close standard file");
		} else {
			f.close();
			return successresult();
		}
	}

	private static Varargs successresult() {
		return TRUE;
	}

	private static Varargs errorresult(Exception ioe) {
		String s = ioe.getMessage();
		return errorresult("io error: " + (s != null ? s : ioe.toString()));
	}

	private static Varargs errorresult(String errortext) {
		return varargsOf(NIL, valueOf(errortext), ZERO);
	}

	private Varargs lines(final File f, final boolean autoClose) {
		return new VarArgFunction((state, args) -> {
			//	lines iterator(s,var) -> var'
			checkopen(f);

			try {
				LuaValue result = freadline(f);
				if (autoClose && result == NIL) ioclose(f);
				return result;
			} catch (IOException e) {
				return errorresult(e);
			}
		});
	}

	private static Varargs iowrite(File f, Varargs args) throws IOException, LuaError {
		for (int i = 1, n = args.count(); i <= n; i++) {
			f.write(args.arg(i).checkLuaString());
		}
		return TRUE;
	}

	private Varargs ioread(File f, Varargs args) throws IOException, LuaError {
		int i, n = args.count();
		if (n == 0) {
			return freadline(f);
		}

		LuaValue[] v = new LuaValue[n];
		LuaValue ai, vi;
		LuaString fmt;
		for (i = 0; i < n; ) {
			item:
			switch ((ai = args.arg(i + 1)).type()) {
				case TNUMBER:
					vi = freadbytes(f, ai.toInteger());
					break;
				case TSTRING:
					fmt = ai.checkLuaString();
					if (fmt.length >= 2 && fmt.bytes[fmt.offset] == '*') {
						switch (fmt.bytes[fmt.offset + 1]) {
							case 'n':
								vi = freadnumber(f);
								break item;
							case 'l':
								vi = freadline(f);
								break item;
							case 'a':
								vi = freadall(f);
								break item;
						}
					}
				default:
					throw ErrorFactory.argError(i + 1, "(invalid format)");
			}
			if ((v[i++] = vi).isNil()) {
				break;
			}
		}
		return i == 0 ? NIL : varargsOf(v, 0, i);
	}

	private static File checkfile(LuaValue val) throws LuaError {
		File f = optfile(val);
		if (f == null) {
			throw ErrorFactory.argError(1, "file");
		}
		checkopen(f);
		return f;
	}

	private static File optfile(LuaValue val) {
		return (val instanceof File) ? (File) val : null;
	}

	private static File checkopen(File file) throws LuaError {
		if (file.isclosed()) {
			throw new LuaError("attempt to use a closed file");
		}
		return file;
	}

	private File rawopenfile(LuaState state, String filename, String mode) throws IOException {
		boolean isstdfile = "-".equals(filename);
		boolean isreadmode = mode.startsWith("r");
		if (isstdfile) {
			return isreadmode ?
				wrapStandardStream(state.stdin) :
				wrapStandardStream(state.stdout);
		}
		boolean isappend = mode.startsWith("a");
		boolean isupdate = mode.indexOf("+") > 0;
		boolean isbinary = mode.endsWith("b");
		return openFile(filename, isreadmode, isappend, isupdate, isbinary);
	}


	// ------------- file reading utilitied ------------------

	public static LuaValue freadbytes(File f, int count) throws IOException {
		byte[] b = new byte[count];
		int r;
		if ((r = f.read(b, 0, b.length)) < 0) {
			return NIL;
		}
		return LuaString.valueOf(b, 0, r);
	}

	public static LuaValue freaduntil(File f, boolean lineonly) throws IOException {
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

	public static LuaValue freadline(File f) throws IOException {
		return freaduntil(f, true);
	}

	public static LuaValue freadall(File f) throws IOException {
		int n = f.remaining();
		if (n >= 0) {
			return freadbytes(f, n);
		} else {
			return freaduntil(f, false);
		}
	}

	public static LuaValue freadnumber(File f) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		freadchars(f, " \t\r\n", null);
		freadchars(f, "-+", baos);
		//freadchars(f,"0",baos);
		//freadchars(f,"xX",baos);
		freadchars(f, "0123456789", baos);
		freadchars(f, ".", baos);
		freadchars(f, "0123456789", baos);
		freadchars(f, "eEfFgG", baos);
		freadchars(f, "+-", baos);
		freadchars(f, "0123456789", baos);
		String s = baos.toString();
		return s.length() > 0 ? valueOf(Double.parseDouble(s)) : NIL;
	}

	private static void freadchars(File f, String chars, ByteArrayOutputStream baos) throws IOException {
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
