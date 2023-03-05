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
end)
