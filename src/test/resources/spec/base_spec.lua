describe("The base library", function()
	describe("tonumber", function()
		it("rejects partial numbers", function()
			local invalid = { "-", " -", "- ", " - ", "0x" }
			for _, k in pairs(invalid) do
				expect(tonumber(k)):describe(("tonumber(%q)"):format(k)):eq(nil)
			end
		end)
	end)
end)
