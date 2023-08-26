describe("The string library", function()
	describe("string.format", function()
		it("'q' option formats edge-case numbers correctly :lua>=5.4", function()
			local formatted = string.format("%q %q %q", 0/0, 1/0, -1/0)
			expect(formatted):eq("(0/0) 1e9999 -1e9999")
		end)

		it("'q' option prints hexadecimal floats :lua>=5.4", function()
			expect(("%q"):format(234.53)):eq("0x1.d50f5c28f5c29p+7")
		end)
	end)

	describe("string.pack", function()
		it("'z' modifier on exactly the buffer boundary :lua>=5.3", function()
			local packed = string.pack("z", ("#"):rep(32))
			expect(packed):eq("################################\0")
		end)
	end)

	describe("string.find", function()
		it("character classes :lua>=5.2", function()
			-- Lua 5.1 doesn't support 'g'.

			local classes = "acdglpsuwx"
			local tbl = {
				--[[ 00 ]] "c", "c", "c", "c", "c", "c", "c", "c", "c", "cs", "cs", "cs", "cs", "cs", "c", "c",
				--[[ 10 ]] "c", "c", "c", "c", "c", "c", "c", "c", "c", "c", "c", "c", "c", "c", "c", "c",
				--[[ 20 ]] "s", "gp", "gp", "gp", "gp", "gp", "gp", "gp", "gp", "gp", "gp", "gp", "gp", "gp", "gp", "gp",
				--[[ 30 ]] "dgwx", "dgwx", "dgwx", "dgwx", "dgwx", "dgwx", "dgwx", "dgwx", "dgwx", "dgwx", "gp", "gp", "gp", "gp", "gp", "gp",
				--[[ 40 ]] "gp", "aguwx", "aguwx", "aguwx", "aguwx", "aguwx", "aguwx", "aguw", "aguw", "aguw", "aguw", "aguw", "aguw", "aguw", "aguw", "aguw",
				--[[ 50 ]] "aguw", "aguw", "aguw", "aguw", "aguw", "aguw", "aguw", "aguw", "aguw", "aguw", "aguw", "gp", "gp", "gp", "gp", "gp",
				--[[ 60 ]] "gp", "aglwx", "aglwx", "aglwx", "aglwx", "aglwx", "aglwx", "aglw", "aglw", "aglw", "aglw", "aglw", "aglw", "aglw", "aglw", "aglw",
				--[[ 70 ]] "aglw", "aglw", "aglw", "aglw", "aglw", "aglw", "aglw", "aglw", "aglw", "aglw", "aglw", "gp", "gp", "gp", "gp", "c",
				--[[ 80 ]] "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
				--[[ 90 ]] "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
				--[[ a0 ]] "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
				--[[ b0 ]] "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
				--[[ c0 ]] "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
				--[[ d0 ]] "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
				--[[ e0 ]] "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
				--[[ f0 ]] "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
			}
			for codepoint = 0, 255 do
				local char = string.char(codepoint)

				local matching = ""
				for i = 1, #classes do
					local class = classes:sub(i, i)
					if char:find("%" .. class) then matching = matching .. class end
				end

				expect(matching)
					:describe(("Character classes for %d (%q)"):format(codepoint, string.char(codepoint)))
					:equals(tbl[codepoint + 1])
			end
		end)
	end)
end)
