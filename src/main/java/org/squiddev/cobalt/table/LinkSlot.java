package org.squiddev.cobalt.table;

import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Varargs;

/**
 * Created by 09CoatJo on 18/03/2016.
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
