describe("The base library", function()
	describe("assert", function()
		it("returns values by default", function()
			expect({assert("a", true, 3)}):same { "a", true, 3 }
		end)

		it("requires at least one argument :lua~=5.2", function()
			expect.error(assert):str_match("bad argument #1"):str_match("value expected")
		end)

		it("defaults to 'assertion failed!'", function()
			expect.error(assert, false):eq("assertion failed!")
		end)

		it("accepts any type as an error message :lua>=5.3", function()
			expect.error(assert, false, 123):eq(123)
		end)
	end)

	describe("tonumber", function()
		it("rejects partial numbers", function()
			local invalid = { "-", " -", "- ", " - ", "0x" }
			for _, k in pairs(invalid) do
				expect(tonumber(k)):describe(("tonumber(%q)"):format(k)):eq(nil)
			end
		end)
	end)

	describe("ipairs", function()
		local function make_slice(tbl, start, len)
			return setmetatable({}, {
				__index = function(self, i)
					if i >= 1 and i <= len then return tbl[start + i - 1] end
				end,
			})
		end

		it("inext returns nil when nothing left :lua>=5.2", function()
			local inext = ipairs({})
			expect(select('#', inext({}, 0))):eq(1)
		end)

		it("uses metamethods :lua>=5.3", function()
			local slice = make_slice({ "a", "b", "c", "d", "e" }, 2, 3)
			local inext = ipairs(slice)

			expect({ inext(slice, 0) }):same { 1, "b" }
			expect({ inext(slice, 1) }):same { 2, "c" }
			expect({ inext(slice, 2) }):same { 3, "d" }
			expect({ inext(slice, 3) }):same { nil }
		end)

		it("uses metamethods on non-table values :lua>=5.3", function()
			local inext = ipairs("hello")
			expect(inext("hello", 0)):eq(nil)
		end)
	end)

	describe("rawequal", function()
		it("two large integers are equal", function()
			-- Older versions of Cobalt uses == instead of .equals, so this was
			-- false.
			expect(rawequal(26463, tonumber("26463"))):eq(true)
		end)
	end)
end)
