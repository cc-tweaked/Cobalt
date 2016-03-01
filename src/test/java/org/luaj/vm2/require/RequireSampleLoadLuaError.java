package org.luaj.vm2.require;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import static org.luaj.vm2.Factory.valueOf;

/**
 * This should fail while trying to load via
 * "require()" because it throws a LuaError
 */
public class RequireSampleLoadLuaError extends ZeroArgFunction {

	public RequireSampleLoadLuaError() {
	}

	@Override
	public LuaValue call() {
		error("sample-load-lua-error");
		return valueOf("require-sample-load-lua-error");
	}
}
