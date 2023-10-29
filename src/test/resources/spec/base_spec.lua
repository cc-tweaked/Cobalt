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

	describe("tostring", function()
		it("nil", function() expect(tostring(nil)):eq("nil") end)

		it("booleans", function()
			expect(tostring(true)):eq("true")
			expect(tostring(false)):eq("false")
		end)

		describe("numbers", function()
			it("basic conversions", function()
				expect(tostring(12)):eq("12")
				expect(""..12):eq('12')
				expect(12 .. ""):eq('12')

				expect(tostring(1234567890123)):eq("1234567890123")
			end)

			it("floats", function()
				expect(tostring(-1203)):eq("-1203")
				expect(tostring(1203.125)):eq("1203.125")
				expect(tostring(-0.5)):eq("-0.5")
				expect(tostring(-32767)):eq("-32767")
			end)

			it("integers :lua>=5.3", function()
				expect(tostring(4611686018427387904)):eq("4611686018427387904")
				expect(tostring(-4611686018427387904)):eq("-4611686018427387904")
			end)

			it("integer-compatible floats are preserved :lua>=5.3 :!cobalt", function()
				expect('' .. 12):eq('12') expect(12.0 .. ''):eq('12.0')
				expect(tostring(-1203 + 0.0)):eq("-1203.0")
			end)

			it("integer-compatible floats are truncated :lua<=5.2", function()
				expect(tostring(0.0)):eq("0")
				expect('' .. 12):eq('12') expect(12.0 .. ''):eq('12')
				expect(tostring(-1203 + 0.0)):eq("-1203")
			end)
		end)

		it("tables", function() expect(tostring {}):str_match('^table:') end)

		it("functions", function() expect(tostring(print)):str_match('^function:') end)

		it("null bytes", function()
			expect(tostring('\0')):eq("\0")
		end)

		it("uses __name :lua>=5.3", function()
			local obj = setmetatable({}, { __name = "abc" })
			expect(tostring(obj)):str_match("^abc: ")
		end)

		it("errors if __tostring does not return a string :lua>=5.3 :!cobalt", function()
			local obj = setmetatable({}, { __tostring = function () return {} end })
			expect.error(tostring, obj):eq("'__tostring' must return a string")
		end)

		it("can return a non-string value :lua<=5.2", function()
			-- Lua 5.3+ requires this to be a string. Which is sensible, but a breaking change!
			local obj = setmetatable({}, { __tostring = function() return false end })
			expect(tostring(obj)):eq(false)
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

	describe("xpcall", function()
		it("accepts multiple values :lua>=5.2", function()
			local ok, res = xpcall(table.pack, function() end, 1, 2, 3)
			expect(ok):eq(true)
			expect(res):same { n = 3, 1, 2, 3 }
		end)
	end)

	describe("load", function()
		it("returns the error value", function()
			local tbl = {}
			local fun, err = load(function() error(tbl) end)

			expect(fun):eq(nil)
			expect(err):eq(tbl)
		end)

		-- I'd hope nobody relies on this behaviour, but you never know!
		it("propagates the current error handler", function()
			local res = {
				xpcall(
					function() return load(function() error("oh no", 0) end) end,
					function(e) return "caught " .. e end
				)
			}
			expect(res):same { true, nil, "caught oh no"}
		end)
	end)
end)
