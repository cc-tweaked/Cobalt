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

import java.lang.ref.WeakReference;

/**
 * A Slot whose value has been set to nil. The key is kept in a weak reference so that
 * it can be found by next().
 */
public class DeadSlot implements Slot {

	private final Object key;
	private Slot next;

	public DeadSlot(LuaValue key, Slot next) {
		this.key = LuaTable.isLargeKey(key) ? new WeakReference<LuaValue>(key) : (Object) key;
		this.next = next;
	}

	private LuaValue key() {
		return (LuaValue) (key instanceof WeakReference ? ((WeakReference) key).get() : key);
	}

	@Override
	public int keyindex(int hashMask) {
		// Not needed: this entry will be dropped during rehash.
		return 0;
	}

	@Override
	public StrongSlot firstSlot() {
		return null;
	}

	@Override
	public StrongSlot find(LuaValue key) {
		return null;
	}

	@Override
	public boolean keyeq(LuaValue key) {
		LuaValue k = key();
		return k != null && key.raweq(k);
	}

	@Override
	public Slot rest() {
		return next;
	}

	@Override
	public int arraykey(int max) {
		return -1;
	}

	@Override
	public Slot set(StrongSlot target, LuaValue value) {
		Slot next = (this.next != null) ? this.next.set(target, value) : null;
		if (key() != null) {
			// if key hasn't been garbage collected, it is still potentially a valid argument
			// to next(), so we can't drop this entry yet.
			this.next = next;
			return this;
		} else {
			return next;
		}
	}

	@Override
	public Slot add(Slot newEntry) {
		return (next != null) ? next.add(newEntry) : newEntry;
	}

	@Override
	public Slot remove(StrongSlot target) {
		if (key() != null) {
			next = next.remove(target);
			return this;
		} else {
			return next;
		}
	}

	@Override
	public Slot relink(Slot rest) {
		return rest;
	}

	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("<dead");
		LuaValue k = key();
		if (k != null) {
			buf.append(": ");
			buf.append(k.toString());
		}
		buf.append('>');
		if (next != null) {
			buf.append("; ");
			buf.append(next.toString());
		}
		return buf.toString();
	}
}
