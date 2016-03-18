package org.squiddev.cobalt.table;

import org.squiddev.cobalt.LuaValue;

/**
 * Represents a slot in the hash table.
 */
public interface Slot {
	Slot[] NOBUCKETS = {};

	/**
	 * Return hash{pow2,mod}( first().key().hashCode(), sizeMask )
	 */
	int keyindex(int hashMask);

	/**
	 * Return first Entry, if still present, or null.
	 */
	StrongSlot firstSlot();

	/**
	 * Compare given key with first()'s key; return first() if equal.
	 */
	StrongSlot find(LuaValue key);

	/**
	 * Compare given key with first()'s key; return true if equal. May
	 * return true for keys no longer present in the table.
	 */
	boolean keyeq(LuaValue key);

	/**
	 * Return rest of elements
	 */
	Slot rest();

	/**
	 * Return first entry's key, iff it is an integer between 1 and max,
	 * inclusive, or zero otherwise.
	 */
	int arraykey(int max);

	/**
	 * Set the value of this Slot's first Entry, if possible, or return a
	 * new Slot whose first entry has the given value.
	 */
	Slot set(StrongSlot target, LuaValue value);

	/**
	 * Link the given new entry to this slot.
	 */
	Slot add(Slot newEntry);

	/**
	 * Return a Slot with the given value set to nil; must not return null
	 * for next() to behave correctly.
	 */
	Slot remove(StrongSlot target);

	/**
	 * Return a Slot with the same first key and value (if still present)
	 * and rest() equal to rest.
	 */
	Slot relink(Slot rest);
}
