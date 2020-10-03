package org.squiddev.cobalt.support;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.lib.FormatDesc;
import org.squiddev.cobalt.lib.UncheckedLuaError;

import java.util.IdentityHashMap;
import java.util.Map;

public class PrettyValue {
	private static final FormatDesc quote = FormatDesc.ofUnsafe("%q");
	private final LuaValue value;

	public PrettyValue(LuaValue value) {
		this.value = value;
	}

	public LuaValue getValue() {
		return value;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		try {
			toString(buffer, new IdentityHashMap<>(), value);
		} catch (LuaError e) {
			throw new UncheckedLuaError(e);
		}
		return buffer.toString();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof PrettyValue && (this == obj || Equality.equals(value, ((PrettyValue) obj).value));
	}

	@Override
	public int hashCode() {
		return value instanceof LuaTable ? 0 : value.hashCode();
	}

	private static void toString(StringBuffer buffer, Map<LuaTable, Integer> seen, LuaValue value) throws LuaError {
		if (value instanceof LuaString) {
			buffer.append('"').append(value).append('"');
		} else if (!(value instanceof LuaTable)) {
			buffer.append(value);
		} else {
			LuaTable table = (LuaTable) value;

			Integer id = seen.get(table);
			boolean skip = id != null;
			if (!skip) {
				id = seen.size();
				seen.put(table, id);
			}

			buffer.append("table#").append(id);
			if (skip) return;

			buffer.append(":={");
			boolean first = true;
			LuaValue key = Constants.NIL;
			while (true) {
				Varargs next = table.next(key);
				key = next.first();
				if (key.isNil()) break;

				if (first) {
					first = false;
				} else {
					buffer.append(", ");
				}

				buffer.append("[");
				toString(buffer, seen, key);
				buffer.append("] = ");
				toString(buffer, seen, table.rawget(key));
			}

			buffer.append("}");
		}
	}
}
