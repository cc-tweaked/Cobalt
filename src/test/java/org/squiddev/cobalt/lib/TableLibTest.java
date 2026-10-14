package org.squiddev.cobalt.lib;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.LoadState;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class TableLibTest {
	private void run(String source) throws Exception {
		LuaState state = new LuaState();
		CoreLibraries.debugGlobals(state);
		state.globals().rawset("proxy", ValueFactory.userdataOf(new Object()));
		LuaThread.runMain(state, LoadState.load(state,
			new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), "=table-like", state.globals()));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"table.insert(proxy, 2, 15); assert(data[2] == 15 and data[3] == 20)",
		"assert(table.remove(proxy, 1) == 10); assert(data[1] == 20 and data[3] == nil)",
		"assert(table.concat(proxy, ',') == '10,20,30')",
		"table.move(proxy, 1, 2, 2); assert(data[2] == 10 and data[3] == 20)",
		"table.sort(proxy, function(a, b) return a > b end); assert(data[1] == 30 and data[3] == 10)",
	})
	public void acceptsTableLikeUserdata(String operation) throws Exception {
		run("""
			local data = {10, 20, 30}
			debug.setmetatable(proxy, {
				__len = function() return #data end,
				__index = data,
				__newindex = data,
			})
			""" + operation);
	}

	@ParameterizedTest
	@ValueSource(strings = {"__index", "__newindex", "__len", "both"})
	public void rejectsMissingRequiredMetamethod(String missing) throws Exception {
		run("""
			local mt = {
				__len = function() return 0 end,
				__index = {},
				__newindex = {},
			}
			""" + (missing.equals("both") ? "mt.__index, mt.__newindex = nil, nil\n" : "mt['" + missing + "'] = nil\n") + """
			debug.setmetatable(proxy, mt)
			local ok, err = pcall(table.insert, proxy, 42)
			assert(not ok and err:find('table expected', 1, true))
			""");
	}

	@ParameterizedTest
	@ValueSource(strings = {"nil", "false"})
	public void readOnlyOperationDoesNotRequireNewindex(String newindex) throws Exception {
		run("""
			debug.setmetatable(proxy, {
				__len = function() return 2 end,
				__index = {'a', 'b'},
			""" + "__newindex = " + newindex + "\n})\n" + """
			assert(table.concat(proxy, ',') == 'a,b')
			""");
	}

	@ParameterizedTest
	@ValueSource(strings = {"__index", "__newindex"})
	public void propagatesMetamethodErrors(String name) throws Exception {
		run("""
			local token = {}
			local mt = {__len = function() return 1 end, __index = {10}, __newindex = {}}
			""" + "mt['" + name + "'] = function() error(token) end\n" + """
			debug.setmetatable(proxy, mt)
			local ok, err = pcall(table.insert, proxy, 1, 42)
			assert(not ok and err == token)
			""");
	}
}
