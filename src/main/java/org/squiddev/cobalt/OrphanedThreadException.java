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
package org.squiddev.cobalt;

/**
 * An {@link InterruptedException} subclass that indicates a Lua thread that is no longer referenced has been detected.
 *
 * The Java thread in which this is thrown should correspond to a {@link LuaThread} being used as a coroutine that could
 * not possibly be resumed again because there are no more references to the coroutine with which it is associated.
 * Rather than locking up resources forever, this error is thrown, and should fall through all the way to top level of
 * the executor.
 *
 * Java code mixed with the Lua VM should not catch this error. This may be thrown at any time, including when other
 * coroutines from the same VM are running, and so you run the risk of breaking thread-safety.
 */
public class OrphanedThreadException extends InterruptedException {
	private static final long serialVersionUID = -611369749534243472L;

	OrphanedThreadException() {
		super("Orphaned thread");
	}
}
