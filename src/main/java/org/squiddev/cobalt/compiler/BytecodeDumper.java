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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Prototype;
import org.squiddev.cobalt.function.LocalVariable;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.squiddev.cobalt.compiler.LuaBytecodeFormat.*;

class BytecodeDumper {
	private static final boolean IS_LITTLE_ENDIAN = true;
	private static final int NUMBER_FORMAT = 0;
	private static final int SIZEOF_LUA_NUMBER = 8;
	private static final int SIZEOF_INT = 4;
	private static final int SIZEOF_SIZET = 4;
	private static final int SIZEOF_INSTRUCTION = 4;

	private final DataOutputStream writer;
	private final boolean strip;

	private BytecodeDumper(OutputStream w, boolean strip) {
		writer = new DataOutputStream(w);
		this.strip = strip;
	}

	private void dumpChar(int b) throws IOException {
		writer.write(b);
	}

	private void dumpInt(int x) throws IOException {
		if (IS_LITTLE_ENDIAN) {
			writer.writeByte(x & 0xff);
			writer.writeByte((x >> 8) & 0xff);
			writer.writeByte((x >> 16) & 0xff);
			writer.writeByte((x >> 24) & 0xff);
		} else {
			writer.writeInt(x);
		}
	}

	private void dumpNullableString(@Nullable LuaString s) throws IOException {
		if (s == null) {
			dumpInt(0);
		} else {
			dumpString(s);
		}
	}

	private void dumpString(LuaString s) throws IOException {
		final int len = s.length();
		dumpInt(len + 1);
		s.write((OutputStream) writer);
		writer.write(0);
	}

	private void dumpDouble(double d) throws IOException {
		long l = Double.doubleToLongBits(d);
		if (IS_LITTLE_ENDIAN) {
			dumpInt((int) l);
			dumpInt((int) (l >> 32));
		} else {
			writer.writeLong(l);
		}
	}

	private void dumpCode(final Prototype f) throws IOException {
		final int[] code = f.code;
		int n = code.length;
		dumpInt(n);
		for (int aCode : code) {
			dumpInt(aCode);
		}
	}

	private void dumpConstants(final Prototype f) throws IOException {
		final LuaValue[] k = f.constants;
		int i, n = k.length;
		dumpInt(n);
		for (i = 0; i < n; i++) {
			final LuaValue o = k[i];
			switch (o.type()) {
				case Constants.TNIL -> writer.write(Constants.TNIL);
				case Constants.TBOOLEAN -> {
					writer.write(Constants.TBOOLEAN);
					dumpChar(o.toBoolean() ? 1 : 0);
				}
				case Constants.TNUMBER -> {
					writer.write(Constants.TNUMBER);
					dumpDouble(o.toDouble());
				}
				case Constants.TSTRING -> {
					writer.write(Constants.TSTRING);
					dumpString((LuaString) o);
				}
				default -> throw new IllegalArgumentException("bad type for " + o);
			}
		}
	}

	private void dumpDebug(final Prototype f) throws IOException {
		if (f.source == null || strip) {
			dumpInt(0);
		} else {
			dumpString(f.source);
		}

		int i, n;
		n = strip ? 0 : f.lineInfo.length;
		dumpInt(n);
		for (i = 0; i < n; i++) {
			dumpInt(f.lineInfo[i]);
		}
		n = strip ? 0 : f.locals.length;
		dumpInt(n);
		for (i = 0; i < n; i++) {
			LocalVariable lvi = f.locals[i];
			dumpString(lvi.name);
			dumpInt(lvi.startpc);
			dumpInt(lvi.endpc);
		}
		n = strip ? 0 : f.upvalues();
		dumpInt(n);
		for (i = 0; i < n; i++) dumpNullableString(f.getUpvalueName(i));
	}

	private void dumpFunction(final Prototype f) throws IOException {
		dumpInt(f.lineDefined);
		dumpInt(f.lastLineDefined);
		dumpChar(f.parameters);
		dumpChar(f.isVarArg ? 1 : 0);
		dumpChar(f.maxStackSize);
		dumpCode(f);
		dumpConstants(f);
		dumpFunctions(f);
		dumpUpvalues(f);
		dumpDebug(f);
	}

	private void dumpUpvalues(Prototype f) throws IOException {
		int n = f.upvalues();
		dumpInt(n);
		for (int i = 0; i < n; i++) {
			var info = f.getUpvalue(i);
			writer.writeBoolean(info.fromLocal());
			writer.writeByte(info.byteIndex());
		}
	}

	private void dumpFunctions(Prototype f) throws IOException {
		int n = f.children.length;
		dumpInt(n);
		for (int i = 0; i < n; i++) {
			dumpFunction(f.children[i]);
		}
	}

	private void dumpHeader() throws IOException {
		writer.write(LuaBytecodeFormat.LUA_SIGNATURE);
		writer.write(LUAC_VERSION);
		writer.write(LUAC_FORMAT);
		writer.write(IS_LITTLE_ENDIAN ? 1 : 0);
		writer.write(SIZEOF_INT);
		writer.write(SIZEOF_SIZET);
		writer.write(SIZEOF_INSTRUCTION);
		writer.write(SIZEOF_LUA_NUMBER);
		writer.write(NUMBER_FORMAT);
		writer.write(LUAC_TAIL);
	}

	/**
	 * Dump Lua functions as a precompiled chunk.
	 *
	 * @param f     The function to dump.
	 * @param w     The output stream to write to.
	 * @param strip Whether to strip debug informtion.
	 * @throws IOException If writing to the underlying stream failed.
	 */
	public static void dump(Prototype f, OutputStream w, boolean strip) throws IOException {
		BytecodeDumper D = new BytecodeDumper(w, strip);
		D.dumpHeader();
		D.dumpFunction(f);
	}
}
