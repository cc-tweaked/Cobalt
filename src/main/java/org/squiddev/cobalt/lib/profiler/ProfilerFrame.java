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

import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.Prototype;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.lib.DebugLib;

/**
 * Stores information about the profiler
 */
public class ProfilerFrame {
	public long functionLocalTimeMarker;
	public long functionTotalTimeMarker;
	public long functionLocalTime;
	public long functionTotalTime;

	public final LuaString functionName;
	public final String sourceCode;
	public final long lineDefined;
	public long currentLine;

	public ProfilerFrame(DebugFrame frame) {
		LuaString[] string = frame.getFuncKind();
		functionName = string == null ? DebugLib.QMARK : string[1];

		if (frame.closure == null) {
			sourceCode = frame.func.debugName();
			lineDefined = -1;
		} else {
			Prototype proto = frame.closure.getPrototype();
			sourceCode = proto.sourceShort().toString();
			lineDefined = proto.linedefined;
		}

	}

	public void computeLocalTime(long time) {
		functionLocalTime += (time - functionLocalTimeMarker);
	}

	public void computeTotalTime(long time) {
		functionTotalTime += (time - functionTotalTimeMarker);
	}
}
