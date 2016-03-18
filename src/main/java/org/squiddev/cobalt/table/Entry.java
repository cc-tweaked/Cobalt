package org.squiddev.cobalt.table;

import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Varargs;

import static org.squiddev.cobalt.Constants.NIL;
import static org.squiddev.cobalt.Constants.NONE;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

/**
 * Base class for regular entries.
 *
 * If the key may be an integer, the {@link #arraykey(int)} method must be
 * overridden to handle that case.
 */
public abstract class Entry extends Varargs implements StrongSlot {
	@Override
	public abstract LuaValue key();

	@Override
	public abstract LuaValue value();

	abstract Entry set(LuaValue value);

	@Override
	public int arraykey(int max) {
		return 0;
	}

	@Override
	public final LuaValue arg(int i) {
		switch (i) {
			case 1:
				return key();
			case 2:
				return value();
		}
		return NIL;
	}

	@Override
	public final int count() {
		return 2;
	}

	/**
	 * Subclasses should redefine as "return this;" whenever possible.
	 */
	@Override
	public Varargs toVarargs() {
		return varargsOf(key(), value());
	}

	@Override
	public final LuaValue first() {
		return key();
	}

	@Override
	public Varargs subargs(int start) {
		switch (start) {
			case 1:
				return this;
			case 2:
				return value();
		}
		return NONE;
	}

	@Override
	public StrongSlot firstSlot() {
		return this;
	}

	@Override
	public Slot rest() {
		return null;
	}

	@Override
	public StrongSlot find(LuaValue key) {
		return keyeq(key) ? this : null;
	}

	@Override
	public Slot set(StrongSlot target, LuaValue value) {
		return set(value);
	}

	@Override
	public Slot add(Slot entry) {
		return new LinkSlot(this, entry);
	}

	@Override
	public Slot remove(StrongSlot target) {
		return new DeadSlot(key(), null);
	}

	@Override
	public Slot relink(Slot rest) {
		return (rest != null) ? new LinkSlot(this, rest) : (Slot) this;
	}
}
