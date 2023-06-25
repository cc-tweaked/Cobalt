describe("The Lua lexer/parser", function()
	describe("numbers", function()
		it("rejects numbers ending in 'f' or 'd'", function()
			-- Java's Double.parseDouble accepts "2f" and "2d" as a valid number. Make sure we don't.
			-- See https://github.com/SquidDev/Cobalt/issues/71
			local fn, res = load("return 2f")
			expect(fn):eq(nil)
		end)
	end)
end)
