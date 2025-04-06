package org.squiddev.cobalt.table;

import org.squiddev.cobalt.*;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.squiddev.cobalt.Constants.NIL;

/**
 * Nasty utility functions for writing table tests.
 */
public final class TableOperations {
	private static final Field nodes;
	private static final Field array;
	private static final Field lastFree;
	private static final Method trySet;

	static {
		Field nodesField, arrayField, lastFreeField;
		Method trySetMethod;
		try {
			nodesField = LuaTable.class.getDeclaredField("keys");
			nodesField.setAccessible(true);

			arrayField = LuaTable.class.getDeclaredField("array");
			arrayField.setAccessible(true);

			lastFreeField = LuaTable.class.getDeclaredField("lastFree");
			lastFreeField.setAccessible(true);

			trySetMethod = LuaTable.class.getDeclaredMethod("trySet", LuaValue.class, LuaValue.class);
			trySetMethod.setAccessible(true);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
		nodes = nodesField;
		array = arrayField;
		lastFree = lastFreeField;
		trySet = trySetMethod;
	}

	private TableOperations() {
	}

	/**
	 * Get a list of all keys in the table.
	 *
	 * @param table The table to get keys on.
	 * @return array of keys in the table
	 * @throws LuaError If iterating the table fails.
	 */
	public static Collection<LuaValue> keys(LuaTable table) throws LuaError {
		List<LuaValue> l = new ArrayList<>();
		LuaValue k = NIL;
		while (true) {
			Varargs n = table.next(k);
			if ((k = n.first()).isNil()) {
				break;
			}
			l.add(k);
		}

		return l;
	}

	/**
	 * Get the length of the hash part of the table.
	 *
	 * @param table The current table.
	 * @return length of the hash part, does not relate to count of objects in the table.
	 */
	public static int getHashLength(LuaTable table) {
		try {
			return Array.getLength(nodes.get(table));
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the length of the array part of the table.
	 *
	 * @param table The current table.
	 * @return length of the array part, does not relate to count of objects in the table.
	 */
	public static int getArrayLength(LuaTable table) {
		try {
			return Array.getLength(array.get(table));
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the length of the hash part of the table.
	 *
	 * @param table The current table.
	 * @return length of the hash part, does not relate to count of objects in the table.
	 */
	public static int getLastFree(LuaTable table) {
		try {
			return (int) lastFree.get(table);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Set a value on a table, with the same behaviour as {@link OperationHelper#setTable(LuaState, LuaValue, LuaValue, LuaValue)}.
	 *
	 * @param table The table to update.
	 * @param key   The key to set.
	 * @param value The value to set.
	 */
	public static void setValue(LuaTable table, LuaValue key, LuaValue value) throws LuaError {
		boolean success;
		try {
			success = (boolean) trySet.invoke(table, key, value);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}

		if (!success) table.rawset(key, value);
	}
}
