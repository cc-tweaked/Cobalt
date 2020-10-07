package org.squiddev.cobalt.persist;

public interface Serializable<T extends Serializable<T>> {
	Serializer<T> getSerializer();

	static Serializer<?> getSerializer(Object object) {
		return object instanceof Serializable ? ((Serializable<?>) object).getSerializer() : null;
	}
}
