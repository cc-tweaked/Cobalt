package org.luaj.vm2;

/**
 * Used to denote a {@link LuaFunction} that is bound to a prototype
 */
public interface PrototypeStorage {
	Prototype getPrototype();
}
