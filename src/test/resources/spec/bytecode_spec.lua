--- Tests for bytecode loading and dumping.
--
-- We don't run these on Cobalt, but they're useful to document the behaviour
-- between different versions.
describe("Lua bytecode :!cobalt", function()
	local load = loadstring or load
	local prog = "return function (x) return function (y) return x + y end end"

	it("does not duplicate the source name :lua~=5.2", function()
		-- Lua 5.2 puts the source name at the start of each function.
		local func = assert(load(prog, ("x"):rep(1000)))
		local code = string.dump(func)
		if #code <= 1000 or #code >= 2000 then
			fail(("Bytecode is %d bytes long"):format(#code))
		end
	end)

	it("functions preserve their source name", function()
		local name = "some chunk name"
		local func = assert(load(prog, name))
		local code = string.dump(func)

		local f = assert(load(code))
		local g = f()
		local h = g(3)
		expect(h(5)):eq(8)

		expect(debug.getinfo(f).source):eq(name)
		expect(debug.getinfo(g).source):eq(name)
		expect(debug.getinfo(h).source):eq(name)
	end)

	it("functions use the provided source when stripped :lua<=5.2", function()
		local name = "some chunk name"
		local func = assert(load(prog, name))
		local code = string.dump(func, true)

		local f = assert(load(code))
		local g = f()
		local h = g(3)
		expect(h(5)):eq(8)

		expect(debug.getinfo(f).source):eq(name)
		expect(debug.getinfo(g).source):eq(name)
		expect(debug.getinfo(h).source):eq(name)
	end)

	it("functions have no source when stripped :lua>=5.3", function()
		local name = "some chunk name"
		local func = assert(load(prog, name))
		local code = string.dump(func, true)

		local f = assert(load(code))
		local g = f()
		local h = g(3)
		expect(h(5)):eq(8)

		expect(debug.getinfo(f).source):eq("=?")
		expect(debug.getinfo(g).source):eq("=?")
		expect(debug.getinfo(h).source):eq("=?")
	end)
end)
