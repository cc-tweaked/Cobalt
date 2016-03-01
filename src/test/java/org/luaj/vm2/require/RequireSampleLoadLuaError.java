package org.luaj.vm2.require;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaState;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

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
