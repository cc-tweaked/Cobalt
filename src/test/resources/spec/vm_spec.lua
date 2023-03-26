describe("The Lua VM", function()
	describe("type errors", function()
		local function mk_name(name) return setmetatable({}, {__name = name}) end

		it("uses __name in comparisons :lua>=5.3", function()
			local a, b = mk_name "type a", mk_name "type b"
			expect.error(function() return a < b end):str_match("attempt to compare type a with type b$")
			expect.error(function() return a < a end):str_match("attempt to compare two type a values$")
		end)

		it("uses __name in argument errors :lua>=5.3", function()
			local a = mk_name "type a"
			expect.error(string.match, a):str_match("string expected, got type a")
		end)
	end)
end)
