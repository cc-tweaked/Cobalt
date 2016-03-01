package org.luaj.vm2.require;

import org.luaj.vm2.LuaState;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import static org.luaj.vm2.Factory.valueOf;

/**
 * This should succeed as a library that can be loaded dynamically via "require()"
 */
public class RequireSampleSuccess extends ZeroArgFunction {

	public RequireSampleSuccess() {
	}

	@Override
	public LuaValue call(LuaState state) {
		return valueOf("require-sample-success");
	}
}
