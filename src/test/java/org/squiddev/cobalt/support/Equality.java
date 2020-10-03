package org.squiddev.cobalt.support;

import org.squiddev.cobalt.*;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

class Equality {
	private final Map<LuaTable, LuaTable> map = new HashMap<>();
	private final Queue<Pair> queue = new ArrayDeque<>();

	public static boolean equals(LuaValue left, LuaValue right) {
		Equality eq = new Equality();
		if (!eq.checkValue(left, right)) return false;

		try {
			Pair pair;
			while ((pair = eq.queue.poll()) != null) {
				if (!eq.checkTables(pair.left, pair.right)) return false;
			}
		} catch (LuaError e) {
			throw new IllegalStateException(e);
		}

		return true;
	}

	private boolean checkValue(LuaValue left, LuaValue right) {
		if (left.equals(right)) return true;
		if (!(left instanceof LuaTable) || !(right instanceof LuaTable)) return false;

		queue.add(new Pair((LuaTable) left, (LuaTable) right));
		return true;
	}

	private boolean checkTables(LuaTable left, LuaTable right) throws LuaError {
		if (map.get(left) == right) return true;
		if (map.containsKey(left) || map.containsKey(right)) return false;

		map.put(left, right);
		map.put(right, left);

		LuaValue key = Constants.NIL;
		while (true) {
			Varargs next = left.next(key);
			key = next.first();
			if (key.isNil()) break;

			LuaValue value = next.arg(2);
			if (!checkValue(value, right.rawget(key))) return false;
		}

		key = Constants.NIL;
		while (true) {
			Varargs next = right.next(key);
			key = next.first();
			if (key.isNil()) break;

			if (left.rawget(key).isNil()) return false;
		}

		return true;
	}

	private static class Pair {
		final LuaTable left;
		final LuaTable right;

		private Pair(LuaTable left, LuaTable right) {
			this.left = left;
			this.right = right;
		}
	}
}
