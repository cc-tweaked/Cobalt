describe("Lua's base operators", function()
	describe("modulo (%)", function()
		it("behaves correctly for large numbers :lua>=5.3", function()
			-- See https://github.com/SquidDev-CC/CC-Tweaked/issues/404
			--
			-- Tests that our behaviour of % is equivalent to Lua 5.3's.

			local large_prime = 48721
			local small_prime = 3
			local exp_prime = small_prime^math.sqrt(large_prime)
			local public_key = exp_prime%large_prime

			expect(public_key):eq(17511)
		end)
	end)

	describe("error messages", function()
		local function mk_adder(k) return function() return 2 + k end end

		it("includes upvalue names in error messages :lua>=5.1 :lua<=5.2", function()
			expect.error(mk_adder("hello")):strip_context():eq("attempt to perform arithmetic on upvalue 'k' (a string value)")
		end)

		it("includes upvalue names in error messages :lua==5.3 :!cobalt", function()
			expect.error(mk_adder("hello")):strip_context():eq("attempt to perform arithmetic on a string value (upvalue 'k')")
		end)

		local function adder(k) return 2 + k end

		it("includes local names in error messages :lua>=5.1 :lua<=5.2", function()
			expect.error(adder, "hello"):strip_context():eq("attempt to perform arithmetic on local 'k' (a string value)")
		end)

		it("includes local names in error messages :lua==5.3 :!cobalt", function()
			expect.error(adder, "hello"):strip_context():eq("attempt to perform arithmetic on a string value (local 'k')")
		end)

		it("includes no information in error messages :lua>=5.4 :!cobalt", function()
			expect.error(mk_adder("hello")):strip_context():eq("attempt to add a 'number' with a 'string'")
			expect.error(adder, "hello"):strip_context():eq("attempt to add a 'number' with a 'string'")
		end)
	end)

	describe("comparison", function()
		local function value(x) if type(x) == "number" then return x else return x.value end end
		local comparable_mt = {
			__lt = function(x, y) return value(x) < value(y) end,
			__le = function(x, y) return value(x) <= value(y) end,
		}

		local function mk(x) return setmetatable({ value = x }, comparable_mt) end

		it("compare homogenous values", function()
			expect(mk(1) < mk(2)):eq(true)
			expect(mk(2) < mk(2)):eq(false)
			expect(mk(3) < mk(2)):eq(false)

			expect(mk(1) <= mk(2)):eq(true)
			expect(mk(2) <= mk(2)):eq(true)
			expect(mk(3) <= mk(2)):eq(false)
		end)

		it("cannot compare heterogenous values :lua<=5.1 :!cobalt", function()
			expect.error(function() return mk(1) < 2 end)
				:str_match(": attempt to compare table with number$")
		end)

		it("cannot compare heterogenous values", function()
			expect.error(function() return "2.0" < 2 end)
				:str_match(": attempt to compare string with number$")
		end)

		it("compare heterogenous values :lua>=5.2", function()
			expect(mk(1) < 2):eq(true)
			expect(mk(2) < 2):eq(false)
			expect(mk(3) < 2):eq(false)

			expect(1 < mk(2)):eq(true)
			expect(2 < mk(2)):eq(false)
			expect(3 < mk(2)):eq(false)

			expect(mk(1) <= 2):eq(true)
			expect(mk(2) <= 2):eq(true)
			expect(mk(3) <= 2):eq(false)

			expect(1 <= mk(2)):eq(true)
			expect(2 <= mk(2)):eq(true)
			expect(3 <= mk(2)):eq(false)
		end)

		it("<= falls back to __lt", function()
			local comparable_mt = { __lt = function(x, y) return value(x) < value(y) end }
			local function mk(x) return setmetatable({ value = x }, comparable_mt) end

			expect(mk(1) <= mk(2)):eq(true)
			expect(mk(2) <= mk(2)):eq(true)
			expect(mk(3) <= mk(2)):eq(false)
		end)
	end)
end)
