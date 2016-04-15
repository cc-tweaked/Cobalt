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

package org.squiddev.cobalt.lib.profiler;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.debug.DebugFrame;

public final class ProfilerStack {
	public static final int MAX_SIZE = 512;

	public static final int DEFAULT_SIZE = 8;

	private static final ProfilerFrame[] EMPTY = new ProfilerFrame[0];

	/**
	 * The top function
	 */
	public int top = -1;

	private ProfilerFrame[] stack = EMPTY;

	/**
	 * Push a new debug info onto the stack
	 *
	 * @return The created info
	 */
	private ProfilerFrame pushInfo(DebugFrame frame) {
		int top = this.top + 1;

		ProfilerFrame[] frames = stack;
		int length = frames.length;
		if (top >= length) {
			if (top >= MAX_SIZE) throw new LuaError("stack overflow");

			int newSize = length == 0 ? DEFAULT_SIZE : length + (length / 2);
			ProfilerFrame[] f = new ProfilerFrame[newSize];
			System.arraycopy(frames, 0, f, 0, frames.length);
			frames = stack = f;
		}

		this.top = top;
		return frames[top] = new ProfilerFrame(top + 1, frame);
	}

	public void pauseLocalTime() {
		stack[top].computeLocalTime(System.nanoTime());
	}

	public void pause() {
		ProfilerFrame[] stack = this.stack;
		int top = this.top;
		if (top < 0) return;
		long time = System.nanoTime();

		stack[top].computeLocalTime(time);
		for (int i = top; i >= 0; i--) {
			stack[i].computeTotalTime(time);
		}
	}

	public void resumeLocalTime() {
		stack[top].functionLocalTimeMarker = System.nanoTime();
	}

	public void resume() {
		ProfilerFrame[] stack = this.stack;
		int top = this.top;
		if (top < 0) return;

		long time = System.nanoTime();

		stack[top].functionLocalTimeMarker = time;
		for (int i = top; i >= 0; i--) {
			stack[i].functionTotalTimeMarker = time;
		}
	}

	public void enter(DebugFrame dFrame) {
		long time = System.nanoTime();
		int top = this.top;
		if (top >= 0) {
			stack[top].computeLocalTime(time);
		}

		ProfilerFrame frame = pushInfo(dFrame);
		frame.functionLocalTimeMarker = time;
		frame.functionTotalTimeMarker = time;
	}

	public ProfilerFrame leave(boolean resume) {
		ProfilerFrame[] stack = this.stack;
		int top = this.top;
		if (top < 0) return null;
		long time = System.nanoTime();

		ProfilerFrame topFrame = stack[top];
		topFrame.computeLocalTime(time);
		topFrame.computeTotalTime(time);

		if (resume && top > 0) {
			stack[top - 1].functionLocalTimeMarker = time;
		}

		stack[top] = null;
		this.top = top - 1;

		return topFrame;
	}
}
