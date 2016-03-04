package org.squiddev.cobalt.require;

import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.function.ZeroArgFunction;

/**
 * This should fail while trying to load via "require()" because it throws a RuntimeException
 */
public class RequireSampleLoadRuntimeExcep extends ZeroArgFunction {

	public RequireSampleLoadRuntimeExcep() {
	}

	@Override
	public LuaValue call(LuaState state) {
		throw new RuntimeException("sample-load-runtime-exception");
	}
}
