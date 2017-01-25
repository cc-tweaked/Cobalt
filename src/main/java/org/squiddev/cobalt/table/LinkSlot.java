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

import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Varargs;

/**
 * A linked list implementation of a slot
 */
public class LinkSlot implements StrongSlot {
	private Entry entry;
	private Slot next;

	LinkSlot(Entry entry, Slot next) {
		this.entry = entry;
		this.next = next;
	}

	@Override
	public LuaValue key() {
		return entry.key();
	}

	@Override
	public int keyindex(int hashMask) {
		return entry.keyindex(hashMask);
	}

	@Override
	public LuaValue value() {
		return entry.value();
	}

	@Override
	public Varargs toVarargs() {
		return entry.toVarargs();
	}

	@Override
	public StrongSlot firstSlot() {
		return entry;
	}

	@Override
	public StrongSlot find(LuaValue key) {
		return entry.keyeq(key) ? this : null;
	}

	@Override
	public boolean keyeq(LuaValue key) {
		return entry.keyeq(key);
	}

	@Override
	public Slot rest() {
		return next;
	}

	@Override
	public int arraykey(int max) {
		return entry.arraykey(max);
	}

	@Override
	public Slot set(StrongSlot target, LuaValue value) {
		if (target == this) {
			entry = entry.set(value);
			return this;
		} else {
			return setnext(next.set(target, value));
		}
	}

	@Override
	public Slot add(Slot entry) {
		return setnext(next.add(entry));
	}

	@Override
	public Slot remove(StrongSlot target) {
		if (this == target) {
			return new DeadSlot(key(), next);
		} else {
			this.next = next.remove(target);
		}
		return this;
	}

	@Override
	public Slot relink(Slot rest) {
		// This method is (only) called during rehash, so it must not change this.next.
		return (rest != null) ? new LinkSlot(entry, rest) : (Slot) entry;
	}

	// this method ensures that this.next is never set to null.
	private Slot setnext(Slot next) {
		if (next != null) {
			this.next = next;
			return this;
		} else {
			return entry;
		}
	}

	public String toString() {
		return entry + "; " + next;
	}
}
