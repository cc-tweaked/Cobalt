package org.squiddev.cobalt.table;

import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Varargs;

/**
 * Subclass of Slot guaranteed to have a strongly-referenced key and value,
 * to support weak tables.
 */
public interface StrongSlot extends Slot {
	/**
	 * Return first entry's key
	 */
	LuaValue key();

	/**
	 * Return first entry's value
	 */
	LuaValue value();

	/**
	 * Return varargsOf(key(), value()) or equivalent
	 */
	Varargs toVarargs();
}
