package org.squiddev.cobalt.table;

import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Varargs;

/**
 * Subclass of Slot guaranteed to have a strongly-referenced key and value,
 * to support weak tables.
 */
public interface StrongSlot extends Slot {
	/**
	 * Returns first entry's key
	 * @return The first entry's value
	 */
	LuaValue key();

	/**
	 * Returns first entry's value
	 * @return The first entry's value
	 */
	LuaValue value();

	/**
	 * Creates a pair with key and value
	 * @return A pair of key and value, stored as a varargs
	 */
	Varargs toVarargs();
}
