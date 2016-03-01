package org.squiddev.cobalt.require;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.lib.ZeroArgFunction;

/**
 * This should fail while trying to load via
 * "require()" because it throws a LuaError
 */
public class RequireSampleLoadLuaError extends ZeroArgFunction {

	public RequireSampleLoadLuaError() {
	}

	@Override
	public LuaValue call(LuaState state) {
		throw new LuaError("sample-load-lua-error");
	}
}
