package org.squiddev.cobalt.table;

import org.squiddev.cobalt.LuaValue;

/**
 * Represents a slot in the hash table.
 */
public interface Slot {
	Slot[] NOBUCKETS = {};

	/**
	 * Return hash{pow2,mod}( first().key().hashCode(), sizeMask )
	 *
	 * @param hashMask The table's hash mask
	 * @return The key index for this slot
	 */
	int keyindex(int hashMask);

	/**
	 * Return first Entry, if still present, or null.
	 *
	 * @return The first slot or {@code null}
	 */
	StrongSlot firstSlot();

	/**
	 * Compare given key with first()'s key; return first() if equal.
	 *
	 * @param key The key to find
	 * @return The found slot or {@code null}
	 */
	StrongSlot find(LuaValue key);

	/**
	 * Compare given key with first()'s key; return true if equal. May
	 * return true for keys no longer present in the table.
	 *
	 * @param key The to check equality with
	 * @return If these keys are equal
	 */
	boolean keyeq(LuaValue key);

	/**
	 * Return rest of elements
	 *
	 * @return The remaining elements
	 */
	Slot rest();

	/**
	 * Return first entry's key, iff it is an integer between 1 and max,
	 * inclusive, or zero otherwise.
	 *
	 * @param max The maximum size of the array
	 * @return The first entry's key
	 */
	int arraykey(int max);

	/**
	 * Set the value of this Slot's first Entry, if possible, or return a
	 * new Slot whose first entry has the given value.
	 *
	 * @param target The slot to set
	 * @param value  The value to store in the value
	 * @return The head slot of the linked list
	 */
	Slot set(StrongSlot target, LuaValue value);

	/**
	 * Link the given new entry to this slot.
	 *
	 * @param newEntry The slot to add
	 * @return The head slot of the linked list
	 */
	Slot add(Slot newEntry);

	/**
	 * Return a Slot with the given value set to nil; must not return null
	 * for next() to behave correctly.
	 *
	 * @param target The slot to remove
	 * @return The head slot of the linked list
	 */
	Slot remove(StrongSlot target);

	/**
	 * Return a Slot with the same first key and value (if still present)
	 * and rest() equal to rest.
	 *
	 * @param rest The rest of the linked list
	 * @return The head slot of the linked list
	 */
	Slot relink(Slot rest);
}
