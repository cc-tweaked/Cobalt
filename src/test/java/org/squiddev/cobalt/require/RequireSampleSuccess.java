package org.squiddev.cobalt.require;

import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.lib.ZeroArgFunction;
import org.squiddev.cobalt.ValueFactory;

import static org.squiddev.cobalt.ValueFactory.valueOf;

/**
 * This should succeed as a library that can be loaded dynamically via "require()"
 */
public class RequireSampleSuccess extends ZeroArgFunction {

	public RequireSampleSuccess() {
	}

	@Override
	public LuaValue call(LuaState state) {
		return ValueFactory.valueOf("require-sample-success");
	}
}
