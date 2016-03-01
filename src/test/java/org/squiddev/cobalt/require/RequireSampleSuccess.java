package org.squiddev.cobalt.require;

import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.lib.ZeroArgFunction;
import org.squiddev.cobalt.Factory;

import static org.squiddev.cobalt.Factory.valueOf;

/**
 * This should succeed as a library that can be loaded dynamically via "require()"
 */
public class RequireSampleSuccess extends ZeroArgFunction {

	public RequireSampleSuccess() {
	}

	@Override
	public LuaValue call(LuaState state) {
		return Factory.valueOf("require-sample-success");
	}
}
