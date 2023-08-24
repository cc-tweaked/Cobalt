describe("The Lua lexer/parser", function()
	describe("numbers", function()
		local load = loadstring or load
		it("rejects numbers ending in 'f' or 'd'", function()
			-- Java's Double.parseDouble accepts "2f" and "2d" as a valid number. Make sure we don't.
			-- See https://github.com/SquidDev/Cobalt/issues/71
			local fn, res = load("return 2f")
			expect(fn):eq(nil)
		end)

		it("rejects malformed hex floating points", function()
			expect(tonumber('0x3.3.3')):eq(nil) -- two decimal points
			expect(tonumber('-0xaaP ')):eq(nil) -- no exponent
			expect(tonumber('0x0.51p')):eq(nil) -- no digits
			expect(tonumber('0x5p+-2')):eq(nil) -- double signs
		end)

		it("parses hex floating points :lua>=5.4", function()
			local function e(x) return assert(load("return " .. x))() end
			expect(e"0x0p12"):eq(0)
			expect(e"0x.0p-3"):eq(0)
			expect(tonumber('  +0x0.51p+8  ')):eq(0x51)
			expect(e"0Xabcdef.0"):eq(e"0x.ABCDEFp+24")
			expect(e"0x1p9999"):eq(math.huge)
			expect(e"0x1.0p-1022"):ne(0)
		end)
	end)
end)
