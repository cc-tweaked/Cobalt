package org.luaj.vm2.require;

import org.luaj.vm2.LuaValue;

import static org.luaj.vm2.Factory.valueOf;

/**
 * This should fail while trying to load via "require() because it is not a LibFunction"
 */
public class RequireSampleClassCastExcep {

	public RequireSampleClassCastExcep() {
	}

	public LuaValue call() {
		return valueOf("require-sample-class-cast-excep");
	}
}
