package org.squiddev.cobalt.persist;

import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Constants for persistence.
 *
 * Saving and loading are handled by {@link ValueWriter} and {@link ValueWriter} respectively. The format is relatively
 * simple, but worth explaining below.
 *
 * Say we wish to write value {@code x}. We may take on of several actions:
 * <ul>
 *     <li>
 *         If this is a simple value (a number, boolean, nil, etc...) we write the value's type and then the value's
 *         representation.
 *     </li>
 *     <li>
 *         If this is a reference value, and we've seen it before, we write {@link Persist#TAG_REFERENCE} and the
 *         value's ID.
 *     </li>
 *     <li>
 *         If we've not seen the value before, we add it to the object-&gt;id table. The id is just an incrementing id, and
 *         so does not need to be written when writing the value.
 *
 *         We now come to write the value's tag. When writing a value we keep track of its depth. Once we're deeper than
 *         {@link ValueWriter#MAX_DEPTH}, we also write {@link Persist#FLAG_PARTIAL} as part of the tag, add it to a
 *         queue and abort. Otherwise we recursively visit this value and write its contents (such as a table's fields,
 *         etc...).
 *     </li>
 * </ul>
 *
 * Once all values have been written, we loop through the queue. Each enqueued value's type is written (ored with
 * {@link Persist#FLAG_PARTIAL}) and then the body written.
 *
 * Reading follows much the same principle (read the main body, and then populate later on). Care must be taken that
 * values are added to the id-&gt;object table before their body is read.
 */
public final class Persist {
	private Persist() {
	}

	static final int FLAG_PARTIAL = 1 << 7;
	static final int FLAG_POPULATE = 1 << 6;
	static final int MASK_TAG = ~(FLAG_PARTIAL | FLAG_POPULATE);

	static final int TAG_NIL = 0;
	static final int TAG_TRUE = 1;
	static final int TAG_FALSE = 2;

	static final int TAG_SHORT_INT = 3;
	static final int TAG_INT = 4;
	static final int TAG_FLOAT = 5;

	static final int TAG_SHORT_STRING = 6;
	static final int TAG_STRING = 7;

	static final int TAG_TABLE = 8;

	static final int TAG_CLOSURE = 9;
	static final int TAG_PROTOTYPE = 10;
	static final int TAG_UPVALUE = 11;
	static final int TAG_STACK = 12;
	static final int TAG_THREAD = 13;

	static final int TAG_SERIALIZER = 14;
	static final int TAG_SERIALIZED = 15;

	static final int TAG_REFERENCE = 20;

	static final int IN_HOOK = 1 << 0;
	static final int HAS_ERRORFUNC = 1 << 1;

	public static void persist(LuaState state, DataOutput stream, LuaValue value) throws IOException {
		try (ValueWriter writer = new ValueWriter(state, stream)) {
			writer.write(value);
		}
	}

	public static LuaValue unpersist(LuaState state, DataInput stream) throws IOException {
		try (ValueReader reader = new ValueReader(state, stream)) {
			return (LuaValue) reader.read();
		}
	}
}
