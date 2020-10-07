package org.squiddev.cobalt.persist;

import java.io.IOException;

public interface Serializer<T extends Serializable<T>> {
	String getName();

	void save(ValueWriter writer, T value) throws IOException;

	T load(ValueReader reader) throws IOException;
}
