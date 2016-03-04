package org.squiddev.cobalt.require;

import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.ValueFactory;
import org.squiddev.cobalt.function.ZeroArgFunction;

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
