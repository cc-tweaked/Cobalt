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
package org.squiddev.cobalt.debug;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.function.LuaFunction;

/**
 * Each thread will get a DebugState attached to it by the debug library
 * which will track function calls, hook functions, etc.
 */
public final class DebugFrame {
	/**
	 * Whether this function contributes to the Java call stack
	 *
	 * @see #flags
	 * @see DebugState#pushJavaInfo()
	 */
	public static final int FLAG_JAVA_STACK = 1 << 0;

	/**
	 * This is a fresh instance of the interpreter. The interpreter
	 * loop should not continue beyond functions marked with this flag.
	 */
	public static final int FLAG_FRESH = 1 << 1;

	/**
	 * Whether this function was tail called. When set, an additional "tail calls..." entry is added to the traceback.
	 *
	 * @see #flags
	 */
	public static final int FLAG_TAIL = 1 << 2;

	/**
	 * If this function is a yielded, protected call. Namely, if one can
	 * {@link Resumable#resumeError(LuaState, Object, LuaError)} into it.
	 *
	 * @see #flags
	 * @see org.squiddev.cobalt.lib.CoroutineLib {@code coroutine.resume} sets this, in order to receive errors from the
	 * child coroutine.
	 * @see org.squiddev.cobalt.lib.BaseLib {@code pcall}/{@code xpcall} set this for obvious reasons.
	 */
	public static final int FLAG_YPCALL = 1 << 3;
	// TODO: Remove this and just have a child interface to Resumable?

	/**
	 * If this function errored. This is a really gross hack to ensure we don't resume into errored
	 * functions again.
	 *
	 * @see #flags
	 * @see org.squiddev.cobalt.lib.BaseLib and the xpcall implementation.
	 */
	public static final int FLAG_ERROR = 1 << 4;

	/**
	 * If the result should be inverted  (due to using lt rather than le).
	 *
	 * @see #flags
	 * @see OperationHelper#le(LuaState, LuaValue, LuaValue)
	 */
	public static final int FLAG_LEQ = 1 << 5;

	/**
	 * Whether this function was suspended by a call to {@link LuaState#handleInterrupt()}.
	 */
	public static final int FLAG_INTERRUPTED = 1 << 6;

	/**
	 * Whether this function is currently within an on-call debug hook.
	 *
	 * @see #flags
	 * @see DebugState#onCall(DebugFrame)
	 */
	static final int FLAG_CALL_HOOK = 1 << 10;

	/**
	 * Whether this function is currently within an on-return debug hook. This is used when resuming a function after a
	 * yield.
	 *
	 * @see #flags
	 * @see DebugState#onReturn(DebugFrame, Varargs)
	 */
	static final int FLAG_RETURN_HOOK = 1 << 11;

	/**
	 * Whether this function is currently within a line debug hook.
	 *
	 * @see #flags
	 * @see DebugState#onInstruction(DebugFrame, int)
	 */
	public static final int FLAG_INSN_HOOK = 1 << 12;

	/**
	 * Whether this function is currently within an instruction count debug hook.
	 *
	 * @see #flags
	 * @see DebugState#onInstruction(DebugFrame, int)
	 */
	public static final int FLAG_LINE_HOOK = 1 << 13;

	/**
	 * Whether this function is currently within any hook.
	 */
	public static final int FLAG_ANY_HOOK = FLAG_CALL_HOOK | FLAG_RETURN_HOOK | FLAG_INSN_HOOK | FLAG_LINE_HOOK;

	/**
	 * The debug info's function
	 */
	public LuaFunction func;

	/**
	 * The debug info's closure
	 */
	public LuaClosure closure;

	/**
	 * The stack for this info
	 */
	public LuaValue[] stack;

	/**
	 * The last item in the upvalue linked list.
	 */
	public Upvalue lastUpvalue;

	public Object state;

	public final DebugFrame previous;

	private static final LuaString TEMPORARY = ValueFactory.valueOf("(*temporary)");

	public Varargs varargs, extras;
	public int pc = -1, oldPc = -1, top = -1;
	public int flags;

	DebugFrame(DebugFrame previous) {
		this.previous = previous;
		func = null;
	}

	public void cleanup() {
		Upvalue upvalue = lastUpvalue;
		while (upvalue != null) upvalue = upvalue.close();
	}

	void clear() {
		func = null;
		closure = null;
		stack = null;
		lastUpvalue = null;
		state = null;
		varargs = extras = null;
		flags = 0;
		oldPc = pc = top = -1;
	}

	public void closeUpvalues(int until) {
		Upvalue upvalue = lastUpvalue;
		while (upvalue != null && upvalue.getIndex() >= until) upvalue = upvalue.close();
		lastUpvalue = upvalue;
	}

	public Upvalue getUpvalue(int slot) {
		Upvalue upvalue = lastUpvalue, next = null;
		// We've got a linked list of the form U(1) <- ... <- U(slot) <- ... <- U(top). Keep
		// walking down the linked list until either:
		while (upvalue != null) {
			if (upvalue.getIndex() == slot) {
				// index == slot => We've found the correct upvalue.
				return upvalue;
			} else if (upvalue.getIndex() < slot) {
				// index < slot => We've not got an upvalue for this slot, create it.
				break;
			} else {
				// index > slot => Continue down the linked list.
				next = upvalue;
				upvalue = next.previous;
			}
		}

		// We're now at a point in the linked list where upvalue.slot < slot < next.slot. We need to insert a new node
		// in the linked list between these two points.
		Upvalue newUpvalue = new Upvalue(stack, slot, upvalue); // Create our new node so that U(prev) <- U(slot)
		// Now update U(next) so that it points to U(slot).
		if (next == null) {
			lastUpvalue = newUpvalue;
		} else {
			next.previous = newUpvalue;
		}
		return newUpvalue;
	}

	/**
	 * Get the current line
	 *
	 * @return The current line the function is on
	 */
	public int currentLine() {
		if (closure == null) return -1;
		int[] li = closure.getPrototype().lineInfo;
		return li == null || pc < 0 || pc >= li.length ? -1 : li[pc];
	}

	/**
	 * Get the kind for this function
	 *
	 * @return This function's kind
	 */
	public @Nullable ObjectName getFuncKind() {
		DebugFrame previous = this.previous;
		if ((flags & FLAG_TAIL) != 0) return null;

		if (previous == null || previous.closure == null || previous.pc < 0) return null;

		int stackpos = (previous.closure.getPrototype().code[previous.pc] >> 6) & 0xff;
		return DebugHelpers.getFuncName(previous, stackpos);
	}

	public @Nullable String sourceLine() {
		return closure == null ? null : closure.getPrototype().shortSource() + ":" + currentLine();
	}

	public LuaString getLocalName(int index) {
		if (closure == null) return null;
		LuaString name = closure.getPrototype().getLocalName(index, pc);
		if (name != null) return name;

		// FIXME: Use top rather than maxstacksize. Sadly it isn't currently updated.
		return index > 0 && index <= closure.getPrototype().maxStackSize ? TEMPORARY : null;
	}
}
