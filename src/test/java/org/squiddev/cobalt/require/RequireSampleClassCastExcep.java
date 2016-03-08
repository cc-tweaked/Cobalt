package org.squiddev.cobalt.require;

import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.ValueFactory;

/**
 * This should fail while trying to load via "require() because it is not a LibFunction"
 */
public class RequireSampleClassCastExcep {

	public RequireSampleClassCastExcep() {
	}

	public LuaValue call() {
		return ValueFactory.valueOf("require-sample-class-cast-excep");
	}
}
