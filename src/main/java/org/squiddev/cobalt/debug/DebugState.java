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

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LuaFunction;

import static org.squiddev.cobalt.debug.DebugFrame.*;

/**
 * DebugState is associated with a Thread
 */
public final class DebugState {
	private static final int HOOK_CALL = 1 << 0;
	private static final int HOOK_RETURN = 1 << 1;
	private static final int HOOK_COUNT = 1 << 2;
	private static final int HOOK_LINE = 1 << 3;

	/**
	 * The maximum size the Lua stack can be
	 */
	public static final int MAX_SIZE = 32767;

	/**
	 * The maximum number of "Java" calls.
	 */
	public static final int MAX_JAVA_SIZE = 200;

	public static final int DEFAULT_SIZE = 8;

	private static final DebugFrame[] EMPTY = new DebugFrame[0];

	/**
	 * The thread's lua state
	 */
	private final LuaState state;

	/**
	 * The top function.
	 * <p>
	 * This is limited by {@link #MAX_SIZE}.
	 */
	int top = -1;

	/**
	 * The number of non-interpreter functions on the stack.
	 * <p>
	 * This is limited by {@link #MAX_JAVA_SIZE}.
	 */
	private int javaCount = 0;

	/**
	 * The stack of debug info
	 */
	private DebugFrame[] stack = EMPTY;

	/**
	 * The current debug hook.
	 */
	private DebugHook hook;
	/**
	 * The mask for which hooks to execute.
	 */
	private int hookMask;

	public boolean inhook;

	/**
	 * Number of instructions to execute
	 */
	public int hookCount;

	/**
	 * Number of instructions executed
	 */
	public int hookPendingCount;

	public DebugState(LuaState state) {
		this.state = state;
	}

	public static DebugState get(LuaState state) {
		return state.getCurrentThread().getDebugState();
	}

	/**
	 * Push a new debug frame onto the stack, marking it as also consuming one or more Java stack frames.
	 *
	 * @return The created info. This should be marked with {@link DebugFrame#FLAG_JAVA_STACK} or {@link DebugFrame#FLAG_FRESH}
	 * by the calling function.
	 * @throws LuaError On a stack overflow
	 */
	public DebugFrame pushJavaInfo() throws LuaError {
		int javaCount = this.javaCount + 1;
		if (javaCount >= MAX_JAVA_SIZE) throw new LuaError("stack overflow");

		DebugFrame frame = pushInfo();
		frame.flags |= FLAG_JAVA_STACK;
		this.javaCount = javaCount;
		return frame;
	}

	/**
	 * Push a new debug info onto the stack
	 *
	 * @return The created info
	 * @throws LuaError On a stack overflow.
	 */
	public DebugFrame pushInfo() throws LuaError {
		int top = this.top + 1;

		DebugFrame[] frames = stack;
		int length = frames.length;
		if (top >= length) {
			if (top >= MAX_SIZE) throw new LuaError("stack overflow");
			int newSize = length == 0 ? DEFAULT_SIZE : Math.min(MAX_SIZE, length + (length / 2));
			DebugFrame[] f = new DebugFrame[newSize];
			System.arraycopy(frames, 0, f, 0, length);
			for (int i = frames.length; i < newSize; ++i) {
				f[i] = new DebugFrame(i > 0 ? f[i - 1] : null);
			}
			frames = stack = f;
		}

		this.top = top;
		return frames[top];
	}

	/**
	 * Pop a debug info off the stack
	 */
	public void popInfo() {
		DebugFrame frame = stack[top--];
		if ((frame.flags & FLAG_JAVA_STACK) != 0) javaCount--;
		assert javaCount >= 0;
		frame.clear();
	}

	/**
	 * Setup the hook
	 *
	 * @param func  The function hook
	 * @param call  Hook on calls
	 * @param line  Hook on lines
	 * @param rtrn  Hook on returns
	 * @param count Number of bytecode operators to use
	 */
	public void setHook(DebugHook func, boolean call, boolean line, boolean rtrn, int count) {
		hook = func;
		hookMask = (call ? HOOK_CALL : 0) | (line ? HOOK_LINE : 0) | (rtrn ? HOOK_RETURN : 0) | (count > 0 ? HOOK_COUNT : 0);
		hookCount = count;
		hookPendingCount = count;
	}

	/**
	 * Get the top debug info
	 *
	 * @return The top debug info or {@code null}
	 */
	public DebugFrame getStack() {
		return top >= 0 ? stack[top] : null;
	}

	/**
	 * Get the top debug info
	 *
	 * @return The top debug info or {@code null}
	 */
	public DebugFrame getStackUnsafe() {
		return stack[top];
	}

	/**
	 * Get the debug info at a particular level
	 *
	 * @param level The level to get at
	 * @return The debug info or {@code null}
	 */
	public DebugFrame getFrame(int level) {
		return level >= 0 && level <= top ? stack[top - level] : null;
	}

	/**
	 * Find the debug info for a function
	 *
	 * @param func The function to find
	 * @return The debug info for this function
	 */
	public DebugFrame findDebugInfo(LuaFunction func) {
		for (int i = top - 1; --i >= 0; ) {
			if (stack[i].func == func) {
				return stack[i];
			}
		}
		return new DebugFrame(func);
	}

	public void onCall(DebugFrame frame, Varargs args) throws UnwindThrowable, LuaError {
		if ((hookMask & HOOK_CALL) == 0 || inhook) return;

		callHook(frame, args);
	}

	private void callHook(DebugFrame frame, Varargs args) throws LuaError, UnwindThrowable {
		inhook = true;
		frame.flags |= FLAG_CALL_HOOK;
		frame.extras = args;

		try {
			hook.onCall(state, this, frame);
		} catch (Exception | VirtualMachineError e) {
			inhook = false;
			throw e;
		}

		inhook = false;
		frame.flags &= ~FLAG_CALL_HOOK;
	}

	/**
	 * Called by Closures and recurring Java functions on return when a runtime error
	 * occurred (and thus the hook should not be called).
	 */
	public void onReturnNoHook() {
		popInfo();
	}

	/**
	 * Called by Closures and recurring Java functions on return
	 *
	 * @param frame The current callstack frame.
	 * @throws LuaError        On a runtime error within the hook.
	 * @throws UnwindThrowable If the hook transfers control to another coroutine.
	 */
	public void onReturn(DebugFrame frame) throws LuaError, UnwindThrowable {
		if ((hookMask & HOOK_RETURN) != 0 && !inhook) returnHook(frame);

		popInfo();

		// Update the old PC if we're returning into a Lua function. This ensures that the line hook runs as expected,
		// without us having to update it even when there's no hook.
		if (hookMask != 0 && top >= 0 && !inhook) {
			DebugFrame returnInto = getStackUnsafe();
			returnInto.oldPc = returnInto.pc;
		}
	}

	private void returnHook(DebugFrame frame) throws LuaError, UnwindThrowable {
		inhook = true;
		frame.flags |= FLAG_RETURN_HOOK;

		try {
			hook.onReturn(state, this, frame);
		} catch (Exception | VirtualMachineError e) {
			inhook = false;
			throw e;
		}

		inhook = false;
		frame.flags &= ~FLAG_RETURN_HOOK;
	}

	/**
	 * Called by Closures on bytecode execution
	 *
	 * @param frame The current callstack frame.
	 * @param pc    The current program counter
	 * @throws LuaError        On a runtime error.
	 * @throws UnwindThrowable If the hook transfers control to another coroutine.
	 */
	public void onInstruction(DebugFrame frame, int pc) throws LuaError, UnwindThrowable {
		frame.pc = pc;
		if (inhook || (hookMask & (HOOK_LINE | HOOK_COUNT)) != 0) onInstructionWorker(frame, pc);
	}

	private void onInstructionWorker(DebugFrame frame, int pc) throws LuaError, UnwindThrowable {
		if (inhook) {
			// If we're in a hook and one of these flags is set, then we are resuming from a yield inside the hook. The
			// hooks have been run at this point, so we just need to clear the flag and continue.
			if ((frame.flags & (FLAG_INSN_HOOK | FLAG_LINE_HOOK)) != 0) {
				inhook = false;
				frame.flags &= ~(FLAG_INSN_HOOK | FLAG_LINE_HOOK);
			}
			return;
		}

		if ((hookMask & (HOOK_LINE | HOOK_COUNT)) == 0) return;

		hookInstruction(frame, pc);
		// We clear these flags after running the hook. This means they don't run during a resume.
		inhook = false;
		frame.flags &= ~(FLAG_INSN_HOOK | FLAG_LINE_HOOK);
	}

	void hookInstruction(DebugFrame frame, int pc) throws LuaError, UnwindThrowable {
		// If there is a instruction hook, and we've not yet run it, then do so!
		if ((hookMask & HOOK_COUNT) != 0 && (frame.flags & FLAG_INSN_HOOK) == 0 && --hookPendingCount == 0) {
			hookPendingCount = hookCount;

			inhook = true;
			frame.flags |= FLAG_INSN_HOOK;
			try {
				hook.onCount(state, this, frame);
			} catch (Exception | VirtualMachineError e) {
				inhook = false;
				throw e;
			}
		}

		// Similarly, if we've got a line hook and we've not yet run it, then do so.
		if ((hookMask & HOOK_LINE) != 0 && (frame.flags & FLAG_LINE_HOOK) == 0) {
			Prototype prototype = frame.closure.getPrototype();
			int newLine = prototype.getLine(pc);
			int oldPc = frame.oldPc;

			/*
				It's important to set this flag here, to denote that we've already *checked* the flag for this iteration
				of the interpreter loop, even if we don't end up executing the flag. Otherwise, the following may happen:

				1. The instruction hook yields (sets FLAG_INSN_HOOK)
				2. When resumed, the line hook is not run as the line doesn't change. We continue to the end of this
				   function, setting oldPc := pc.
				4. The interpreter is resumed and is immediately interrupted/suspended.
				5. We resume again. Now, as oldPc <= pc, the line hook is run.
			*/
			frame.flags |= FLAG_LINE_HOOK;

			if (pc <= oldPc || newLine != prototype.getLine(oldPc)) {
				inhook = true;
				try {
					hook.onLine(state, this, frame, newLine);
				} catch (Exception | VirtualMachineError e) {
					inhook = false;
					throw e;
				}
			}
		}

		frame.oldPc = pc;
	}

	/**
	 * The hook function to call
	 */
	public DebugHook getHook() {
		return hook;
	}

	/**
	 * Which item hooks should be called on
	 */
	public boolean hasCallHook() {
		return (hookMask & HOOK_CALL) != 0;
	}

	public boolean hasLineHook() {
		return (hookMask & HOOK_LINE) != 0;
	}

	public boolean hasReturnHook() {
		return (hookMask & HOOK_RETURN) != 0;
	}

	@SuppressWarnings("unchecked")
	public Varargs resume(DebugFrame frame, Varargs args) throws LuaError, UnwindThrowable {
		// Continue executing the instruction hook.
		if ((frame.flags & (FLAG_INSN_HOOK | FLAG_LINE_HOOK)) != 0) hookInstruction(frame, frame.pc);

		if (!(frame.func instanceof Resumable<?>)) {
			throw new NonResumableException(frame.func == null ? "null" : frame.func.debugName());
		}

		return ((Resumable<Object>) frame.func).resume(state, frame, frame.state, args);
	}

	@SuppressWarnings("unchecked")
	public Varargs resumeError(DebugFrame frame, LuaError error) throws LuaError, UnwindThrowable {
		if (!(frame.func instanceof Resumable<?>)) {
			throw new NonResumableException(frame.func == null ? "null" : frame.func.debugName());
		}

		return ((Resumable<Object>) frame.func).resumeError(state, frame, frame.state, error);
	}
}
