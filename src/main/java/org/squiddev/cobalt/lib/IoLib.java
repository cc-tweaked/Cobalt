/*
 * ****************************************************************************
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2016 SquidDev
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

	private static final int IO_CLOSE = 0;
	private static final int IO_FLUSH = 1;
	private static final int IO_INPUT = 2;
	private static final int IO_LINES = 3;
	private static final int IO_OPEN = 4;
	private static final int IO_OUTPUT = 5;
	private static final int IO_POPEN = 6;
	private static final int IO_READ = 7;
	private static final int IO_TMPFILE = 8;
	private static final int IO_TYPE = 9;
	private static final int IO_WRITE = 10;

	private static final int FILE_CLOSE = 11;
	private static final int FILE_FLUSH = 12;
	private static final int FILE_LINES = 13;
	private static final int FILE_READ = 14;
	private static final int FILE_SEEK = 15;
	private static final int FILE_SETVBUF = 16;
	private static final int FILE_WRITE = 17;

	public static final String[] IO_NAMES = {
		"close",
		"flush",
		"input",
		"lines",
		"open",
		"output",
		"popen",
		"read",
		"tmpfile",
		"type",
		"write",
	};

	public static final String[] FILE_NAMES = {
		"close",
		"flush",
		"lines",
		"read",
		"seek",
		"setvbuf",
		"write",
	};

	private LuaTable filemethods;

	public IoLib() {
	}

	@Override
	public LuaValue add(LuaState state, LuaTable env) {

		// io lib functions
		LuaTable t = new LuaTable();
		LibFunction.bind(state, t, IoLibV.class, IO_NAMES, IoLib.class, this);

		// setup streams
		t.rawset(STDIN, input(state));
		t.rawset(STDOUT, output(state));
		t.rawset(STDERR, errput(state));

		// create file methods table
		filemethods = new LuaTable();
		LibFunction.bindOffset(state, filemethods, IoLibV.class, FILE_NAMES, FILE_CLOSE, IoLib.class, this);

		// setup library and index
		filemethods.rawset("__index", filemethods);

		// return the table
		env.set(state, "io", t);
		state.loadedPackages.set(state, "io", t);
		return t;
	}

	private static final class IoLibV extends VarArgFunction {
		private final IoLib iolib;

		public IoLibV(IoLib iolib) {
			this.iolib = iolib;
		}

		@Override
		public Varargs invoke(LuaState state, Varargs args) {
			try {
				switch (opcode) {
					case IO_FLUSH:
						return iolib._io_flush(state);
					case IO_TMPFILE:
						return iolib._io_tmpfile();
					case IO_CLOSE:
						return iolib._io_close(state, args.first());
					case IO_INPUT:
						return iolib._io_input(state, args.first());
					case IO_OUTPUT:
						return iolib._io_output(state, args.first());
					case IO_TYPE:
						return iolib._io_type(args.first());
					case IO_POPEN:
						return iolib._io_popen(args.arg(1).checkString(), args.arg(2).optString("r"));
					case IO_OPEN:
						return iolib._io_open(state, args.arg(1).checkString(), args.arg(2).optString("r"));
					case IO_LINES:
						return iolib._io_lines(state, args.exists(1) ? args.arg(1).checkString() : null);
					case IO_READ:
						return iolib._io_read(state, args);
					case IO_WRITE:
						return iolib._io_write(state, args);

					case FILE_CLOSE:
						return iolib._file_close(args.first());
					case FILE_FLUSH:
						return iolib._file_flush(args.first());
					case FILE_SETVBUF:
						return iolib._file_setvbuf(args.first(), args.arg(2).checkString(), args.arg(3).optInteger(1024));
					case FILE_LINES:
						return iolib._file_lines(args.first());
					case FILE_READ:
						return iolib._file_read(args.first(), args.subargs(2));
					case FILE_SEEK:
						return iolib._file_seek(args.first(), args.arg(2).optString("cur"), args.arg(3).optInteger(0));
					case FILE_WRITE:
						return iolib._file_write(args.first(), args.subargs(2));
				}
			} catch (IOException ioe) {
				return errorresult(ioe);
			}
			return NONE;
		}
	}

	private File input(LuaState state) {
		return infile != null ? infile : (infile = ioopenfile(state, "-", "r"));
	}

	//	io.flush() -> bool
	public Varargs _io_flush(LuaState state) throws IOException {
		checkopen(output(state));
		outfile.flush();
		return TRUE;
	}

	//	io.tmpfile() -> file
	public Varargs _io_tmpfile() throws IOException {
		return tmpFile();
	}

	//	io.close([file]) -> void
	public Varargs _io_close(LuaState state, LuaValue file) throws IOException {
		File f = file.isNil() ? output(state) : checkfile(file);
		checkopen(f);
		return ioclose(f);
	}

	//	io.input([file]) -> file
	public Varargs _io_input(LuaState state, LuaValue file) {
		if (file.isNil()) {
			return input(state);
		} else {
			return infile = file.isString() ? ioopenfile(state, file.checkString(), "r") : checkfile(file);
		}
	}

	// io.output(filename) -> file
	public Varargs _io_output(LuaState state, LuaValue filename) {
		if (filename.isNil()) {
			return output(state);
		} else {
			return outfile = filename.isString() ? ioopenfile(state, filename.checkString(), "w") : checkfile(filename);
		}
	}

	//	io.type(obj) -> "file" | "closed file" | nil
	public Varargs _io_type(LuaValue obj) {
		File f = optfile(obj);
		return f != null ?
			f.isclosed() ? CLOSED_FILE : FILE :
			NIL;
	}

	// io.popen(prog, [mode]) -> file
	public Varargs _io_popen(String prog, String mode) throws IOException {
		return openProgram(prog, mode);
	}

	//	io.open(filename, [mode]) -> file | nil,err
	public Varargs _io_open(LuaState state, String filename, String mode) throws IOException {
		return rawopenfile(state, filename, mode);
	}

	//	io.lines(filename) -> iterator
	public Varargs _io_lines(LuaState state, String filename) {
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
	public Varargs _io_read(LuaState state, Varargs args) throws IOException {
		checkopen(input(state));
		return ioread(infile, args);
	}

	//	io.write(...) -> void
	public Varargs _io_write(LuaState state, Varargs args) throws IOException {
		checkopen(output(state));
		return iowrite(outfile, args);
	}

	// file:close() -> void
	public Varargs _file_close(LuaValue file) throws IOException {
		return ioclose(checkfile(file));
	}

	// file:flush() -> void
	public Varargs _file_flush(LuaValue file) throws IOException {
		checkfile(file).flush();
		return TRUE;
	}

	// file:setvbuf(mode,[size]) -> void
	public Varargs _file_setvbuf(LuaValue file, String mode, int size) {
		checkfile(file).setvbuf(mode, size);
		return TRUE;
	}

	// file:lines() -> iterator
	public Varargs _file_lines(LuaValue file) {
		return lines(checkfile(file), false);
	}

	//	file:read(...) -> (...)
	public Varargs _file_read(LuaValue file, Varargs subargs) throws IOException {
		return ioread(checkfile(file), subargs);
	}

	//  file:seek([whence][,offset]) -> pos | nil,error
	public Varargs _file_seek(LuaValue file, String whence, int offset) throws IOException {
		return valueOf(checkfile(file).seek(whence, offset));
	}

	//	file:write(...) -> void
	public Varargs _file_write(LuaValue file, Varargs subargs) throws IOException {
		return iowrite(checkfile(file), subargs);
	}

	private File output(LuaState state) {
		return outfile != null ? outfile : (outfile = ioopenfile(state, "-", "w"));
	}

	private File errput(LuaState state) {
		return errfile != null ? errfile : (errfile = ioopenfile(state, "-", "w"));
	}

	private File ioopenfile(LuaState state, String filename, String mode) {
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
		return new VarArgFunction() {

			@Override
			public Varargs invoke(LuaState state, Varargs args) {
				//	lines iterator(s,var) -> var'
				checkopen(f);

				try {
					LuaValue result = freadline(f);
					if (autoClose && result == NIL) ioclose(f);
					return result;
				} catch (IOException e) {
					return errorresult(e);
				}
			}
		};
	}

	private static Varargs iowrite(File f, Varargs args) throws IOException {
		for (int i = 1, n = args.count(); i <= n; i++) {
			f.write(args.arg(i).checkLuaString());
		}
		return TRUE;
	}

	private Varargs ioread(File f, Varargs args) throws IOException {
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

	private static File checkfile(LuaValue val) {
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

	private static File checkopen(File file) {
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
