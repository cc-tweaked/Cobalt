/*
 * ****************************************************************************
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2017 SquidDev
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

package org.squiddev.cobalt.table;

import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;

/**
 * Provides operations that depend on the __mode key of the metatable.
 */
public interface Metatable {
	/**
	 * Returns whether or not this table's keys are weak.
	 *
	 * @return Whether this table's keys are weak.
	 */
	boolean useWeakKeys();

	/**
	 * Returns whether or not this table's values are weak.
	 *
	 * @return Whether this table's values are weak.
	 */
	boolean useWeakValues();

	/**
	 * Returns this metatable as a LuaValue.
	 *
	 * @return This metatable as a LuaValue
	 */
	LuaTable toLuaValue();

	/**
	 * Builts a slot appropriate for the given key and value.
	 *
	 * @param key   The new slot's key
	 * @param value The new slot's value
	 * @return The built instance
	 */
	Slot entry(LuaValue key, LuaValue value);

	/**
	 * Wraps the given value in a weak reference if appropriate.
	 *
	 * @param value The value to wrap
	 * @return The wrapped value
	 */
	LuaValue wrap(LuaValue value);

	/**
	 * Returns the value at the given index in the array, or null if it is a weak reference that
	 * has been dropped.
	 *
	 * @param array The array to get it from
	 * @param index The index to get the value from
	 * @return The value at this point or {@code null} if it is no longer there.
	 */
	LuaValue arrayGet(LuaValue[] array, int index);
}
