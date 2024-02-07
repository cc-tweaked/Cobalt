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
end)
