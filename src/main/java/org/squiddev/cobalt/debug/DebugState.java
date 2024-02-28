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
import org.squiddev.cobalt.function.Dispatch;

import java.util.Arrays;

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
	 *
	 * @see #pushInfo()
	 */
	private static final int MAX_SIZE = 32768;

	/**
	 * The maximum size the Lua stack can be in the event of an error.
	 *
	 * @see #growStackIfError()
	 */
	private static final int MAX_ERROR_SIZE = MAX_SIZE + 10;

	/**
	 * The maximum number of "Java" calls.
	 *
	 * @see #pushJavaInfo()
	 */
	private static final int MAX_JAVA_SIZE = 200;

	private static final int DEFAULT_SIZE = 8;

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
		if (top >= length) frames = stack = growStackOrOverflow(frames, top);
		this.top = top;
		return frames[top];
	}

	/**
	 * Grow the stack by a factor of 1.5, throwing {@code stack overflow} if we execed {@link #MAX_SIZE}.
	 */
	private static DebugFrame[] growStackOrOverflow(DebugFrame[] frames, int top) throws LuaError {
		if (top >= MAX_SIZE) throw new LuaError("stack overflow");
		int length = frames.length;
		return growStack(frames, length == 0 ? DEFAULT_SIZE : Math.min(MAX_SIZE, length + (length / 2)));
	}

	/**
	 * Grow the stack ignoring the maximum limit.
	 */
	private static DebugFrame[] growStack(DebugFrame[] frames, int newSize) {
		// Create the new stack, copy the original elements, and then insert new ones with a reference to the previous
		// entry.
		// TODO: Remove "previous"
		DebugFrame[] newFrames = new DebugFrame[newSize];
		System.arraycopy(frames, 0, newFrames, 0, frames.length);
		for (int i = frames.length; i < newSize; ++i) newFrames[i] = new DebugFrame(i > 0 ? newFrames[i - 1] : null);
		return newFrames;
	}

	/**
	 * Add additional space onto the stack for the error handler, in the event of a stack overflow.
	 * <p>
	 * This should be paired with a {@link #shrinkStackIfError()}.
	 */
	public void growStackIfError() {
		var stack = this.stack;
		// If we're at the top of the stack, and we're not already handling an error, grow it.
		if (top == MAX_SIZE - 1 && stack.length == MAX_SIZE) this.stack = growStack(stack, MAX_ERROR_SIZE);
	}

	/**
	 * Shrink the stack again after exiting from the error function.
	 */
	public void shrinkStackIfError() {
		var stack = this.stack;

		int max = Math.min(Math.max(DEFAULT_SIZE, top) * 3, MAX_SIZE);
		// If thread is currently not handling a stack overflow and its size is larger than maximum "reasonable" size,
		// shrink it.
		if (top < MAX_SIZE && stack.length > max) {
			this.stack = Arrays.copyOf(stack, Math.min(top * 2, MAX_SIZE));
		}
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

	public void onCall(DebugFrame frame) throws UnwindThrowable, LuaError {
		if ((hookMask & HOOK_CALL) == 0 || inhook) return;

		callHook(frame);
	}

	private void callHook(DebugFrame frame) throws LuaError, UnwindThrowable {
		inhook = true;
		frame.flags |= FLAG_CALL_HOOK;

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

		if ((hookMask & HOOK_LINE) != 0 && top >= 0) {
			// Update the old PC if we're returning into a Lua function. This ensures that the line hook runs as expected,
			// without us having to update it even when there's no hook.
			DebugFrame returnInto = stack[top];
			returnInto.oldPc = returnInto.pc;
		}
	}

	/**
	 * Called by Closures and recurring Java functions on return
	 *
	 * @param frame The current callstack frame.
	 * @throws LuaError        On a runtime error within the hook.
	 * @throws UnwindThrowable If the hook transfers control to another coroutine.
	 */
	public void onReturn(DebugFrame frame, Varargs result) throws LuaError, UnwindThrowable {
		if ((hookMask & HOOK_RETURN) != 0 && !inhook) returnHook(frame, result);
		onReturnNoHook();
	}

	private void returnHook(DebugFrame frame, Varargs result) throws LuaError, UnwindThrowable {
		inhook = true;
		frame.flags |= FLAG_RETURN_HOOK;

		try {
			hook.onReturn(state, this, frame);
		} catch (UnwindThrowable e) {
			frame.extras = result;
			throw e;
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
		// TODO: Can we avoid the inhook here?
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
			int newLine = prototype.lineAt(pc);
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

			if (pc <= oldPc || newLine != prototype.lineAt(oldPc)) {
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
		int flags = frame.flags;

		// Continue executing the instruction hook.
		if ((flags & (FLAG_INSN_HOOK | FLAG_LINE_HOOK)) != 0) hookInstruction(frame, frame.pc);

		if ((flags & FLAG_CALL_HOOK) != 0) {
			// We yielded within the call hook: extract the arguments from the state and then execute.
			assert inhook;
			inhook = false;
			frame.flags &= ~FLAG_CALL_HOOK;

			// Reset the state and invoke the main function
			Varargs result = Dispatch.invokeFrame(state, frame);
			onReturn(frame, result);
			return result;
		} else if ((flags & FLAG_RETURN_HOOK) != 0) {
			// We yielded within the return hook, so now can just return normally.
			assert inhook;
			inhook = false;
			frame.flags &= ~FLAG_RETURN_HOOK;

			// Just pop the frame
			Varargs result = frame.extras;
			onReturnNoHook();
			return result;
		} else if (!(frame.func instanceof Resumable<?>)) {
			throw new NonResumableException(frame.func == null ? "null" : frame.func.debugName());
		} else {
			Varargs result = ((Resumable<Object>) frame.func).resume(state, frame.state, args);
			onReturn(getStackUnsafe(), result);
			return result;
		}
	}

	@SuppressWarnings("unchecked")
	public Varargs resumeError(DebugFrame frame, LuaError error) throws LuaError, UnwindThrowable {
		if ((frame.flags & FLAG_ANY_HOOK) != 0) throw error;

		if (!(frame.func instanceof Resumable<?>)) {
			throw new NonResumableException(frame.func == null ? "null" : frame.func.debugName());
		}

		Varargs result = ((Resumable<Object>) frame.func).resumeError(state, frame.state, error);
		onReturn(getStackUnsafe(), result);
		return result;
	}
}
