describe("The utf8 library :lua>=5.3", function()
	local valid_strings = {
		{ "", {} },
		{ "hello World", { string.byte("hello World", 1, -1) } },
		{ "汉字/漢字", {27721, 23383, 47, 28450, 23383,} },

		-- minimum and maximum values for each sequence size
		{ "\0\x7F\xC2\x80\xDF\xBF\xE0\xA0\x80\xEF\xBF\xBF\xF0\x90\x80\x80\xF4\x8F\xBF\xBF", {0,0x7F, 0x80,0x7FF, 0x800,0xFFFF, 0x10000,0x10FFFF} },
		{ "日本語a-4\0éó", {26085, 26412, 35486, 97, 45, 52, 0, 233, 243} },

		-- Supplementary Characters
		{ "𣲷𠜎𠱓𡁻𠵼ab𠺢", {0x23CB7, 0x2070E, 0x20C53, 0x2107B, 0x20D7C, 0x61, 0x62, 0x20EA2,} },
		{ "𨳊𩶘𦧺𨳒𥄫𤓓\xF4\x8F\xBF\xBF", {0x28CCA, 0x29D98, 0x269FA, 0x28CD2, 0x2512B, 0x244D3, 0x10ffff} },
	}

	local invalid_strings = {
		-- UTF-8 representation for 0x11ffff (value out of valid range)
		"\xF4\x9F\xBF\xBF",

		-- overlong sequences
		"\xC0\x80",          -- zero
		"\xC1\xBF",          -- 0x7F (should be coded in 1 byte)
		"\xE0\x9F\xBF",      -- 0x7FF (should be coded in 2 bytes)
		"\xF0\x8F\xBF\xBF",  -- 0xFFFF (should be coded in 3 bytes)

		-- invalid bytes
		"\x80",  -- continuation byte
		"\xBF",  -- continuation byte
		"\xFE",  -- invalid byte
		"\xFF",  -- invalid byte
	}

	describe("utf8.offset", function()
		it("returns nil when out of bounds", function()
			expect(utf8.offset("alo", 5)):eq(nil)
			expect(utf8.offset("alo", -4)):eq(nil)
		end)

		it("errors when out of range :lua>=5.4", function()
			-- Error message is "position out of range" on Lua 5.3
			expect.error(utf8.offset, "abc", 1, 5):str_match("position out of bounds")
			expect.error(utf8.offset, "abc", 1, -4):str_match("position out of bounds")
			expect.error(utf8.offset, "", 1, 2):str_match("position out of bounds")
			expect.error(utf8.offset, "", 1, -1):str_match("position out of bounds")
		end)

		it("errors on continuation bytes", function()
			expect.error(utf8.offset, "𦧺", 1, 2):str_match("continuation byte")
			expect.error(utf8.offset, "𦧺", 1, 2):str_match("continuation byte")
			expect.error(utf8.offset, "\x80", 1):str_match("continuation byte")
		end)
	end)

	describe("utf8.len", function()
		it("works with various strings", function()
			for _, str in pairs(valid_strings) do
				local s, t = str[1], str[2]

				local l = utf8.len(s)
				expect(l):describe(s):eq(#t):eq(#string.gsub(s, "[\x80-\xBF]", ""))
			end
		end)

		it("returns nil and a position on malformed input", function()
			expect({ utf8.len "abc\xE3def" }):same { nil, 4 }
			expect({ utf8.len "汉字\x80" }):same { nil, #("汉字") + 1 }
			expect({ utf8.len "\xF4\x9F\xBF" }):same { nil, 1 }
			expect({ utf8.len "\xF4\x9F\xBF\xBF" }):same { nil, 1 }
		end)

		it("returns nil on invalid character", function()
			for _, str in pairs(invalid_strings) do
				expect(utf8.len(str)):describe(("For %q"):format(str)):eq(nil)
			end
		end)
	end)

	describe("utf8.codepoint", function()
		local s = "áéí\128"

		it("returns the expected value", function()
			expect({utf8.codepoint(s, 1, #s - 1)}):same { 225, 233, 237 }
		end)

		it("works with various strings", function()
			for _, str in pairs(valid_strings) do
				local s, t = str[1], str[2]
				expect({utf8.codepoint(s, 1, -1)}):describe(s):same(t)
			end
		end)

		it("returns an empty table for empty bounds", function()
			expect({utf8.codepoint(s, 4, 3)}):same {}
		end)

		it("errors on invalid codepoints", function()
			expect.error(utf8.codepoint, s, 1, #s):eq("invalid UTF-8 code")

			for _, str in pairs(invalid_strings) do
				expect.error(utf8.codepoint, str):describe(("For %q"):format(str))
					:eq("invalid UTF-8 code")
			end
		end)

		it("errors on out of bounds :lua>=5.4", function()
			expect.error(utf8.codepoint, s, #s + 1):str_match("out of bounds")
			expect.error(utf8.codepoint, s, -(#s + 1), 1):str_match("out of bounds")
			expect.error(utf8.codepoint, s, 1, #s + 1):str_match("out of bounds")
		end)
	end)

	describe("utf8.codes", function()
		it("errors on invalid codepoints", function()
			local s = "ab\xff"
			expect.error(function() for i in utf8.codes(s) do assert(i) end end)
				:str_match(": invalid UTF%-8 code$")
		end)
	end)

	describe("utf8.char", function()
		it("works with various strings", function()
			for _, str in pairs(valid_strings) do
				local s, t = str[1], str[2]
				expect(utf8.char(table.unpack(t))):describe(s):eq(s)
			end
		end)

		it("accepts multiple values", function()
			expect(utf8.char()):eq("")
			expect(utf8.char(97, 98, 99)):eq("abc")
		end)

		it("roundtrips with utf8.codepoint", function()
			expect(utf8.codepoint(utf8.char(0x10FFFF))):eq(0x10FFFF)
		end)

		it("errors on out of range values", function()
			expect.error(utf8.char, 0x7FFFFFFF + 1):str_match("value out of range")
			expect.error(utf8.char, -1):str_match("value out of range")
		end)

		it("allows ranges beyond 0x10FFFF :lua>=5.4 :!cobalt", function()
			expect(utf8.char(0x10FFFF + 1)):eq("\244\144\128\128")
		end)
	end)

	describe("utf8.charpattern", function()
		it("behaves correctly", function()
			local x = "日本語a-4\0éó"
			local i = 0
			for p, c in string.gmatch(x, "()(" .. utf8.charpattern .. ")") do
				i = i + 1
				expect(utf8.offset(x, i)):eq(p)
				expect(utf8.len(x, p)):eq(utf8.len(x) - i + 1)
				expect(utf8.len(c)):eq(1)
				for j = 1, #c - 1 do
					expect(utf8.offset(x, 0, p + j - 1)):eq(p)
				end
			end
		end)
	end)

	it("lexing", function()
		for _, str in pairs(valid_strings) do
			local s, t = str[1], str[2]

			local ts = {"return '"}
			for i = 1, #t do ts[i + 1] = string.format("\\u{%x}", t[i]) end
			ts[#t + 2] = "'"
			expect(assert(load(table.concat(ts)))()):eq(s)
		end
	end)

	describe("various test strings", function()
		-- TODO: We really should split this up into more granular tests for each
		-- function.
		for _, str in pairs(valid_strings) do
			local s, t = str[1], str[2]
			it(("%q"):format(s), function()
				expect(utf8.offset(s, 0)):eq(1)

				local l = #t
				for i = 1, l do
					local pi = utf8.offset(s, i)        -- position of i-th char
					local pi1 = utf8.offset(s, 2, pi)   -- position of next char
					assert(string.find(string.sub(s, pi, pi1 - 1), "^" .. utf8.charpattern .. "$"))
					expect(utf8.offset(s, -1, pi1)):eq(pi)
					expect(utf8.offset(s, i - l - 1)):eq(pi)
					expect(pi1 - pi):eq(#utf8.char(utf8.codepoint(s, pi)))
					for j = pi, pi1 - 1 do
						expect(utf8.offset(s, 0, j)):eq(pi)
					end
					for j = pi + 1, pi1 - 1 do
						assert(not utf8.len(s, j))
					end
					expect(utf8.len(s, pi, pi)):eq(1)
					expect(utf8.len(s, pi, pi1 - 1)):eq(1)
					expect(utf8.len(s, pi)):eq(l - i + 1)
					expect(utf8.len(s, pi1)):eq(l - i)
					expect(utf8.len(s, 1, pi)):eq(i)
				end

				local i = 0
				for p, c in utf8.codes(s) do
					i = i + 1
					expect(c):eq(t[i]) expect(p):eq(utf8.offset(s, i))
					expect(utf8.codepoint(s, p)):eq(c)
				end
				expect(i):eq(#t)

				i = 0
				for p, c in utf8.codes(s) do
					i = i + 1
					expect(c):eq(t[i]) expect(p):eq(utf8.offset(s, i))
				end
				expect(i):eq(#t)

				i = 0
				for c in string.gmatch(s, utf8.charpattern) do
					i = i + 1
					expect(c):eq(utf8.char(t[i]))
				end
				expect(i):eq(#t)

				for i = 1, l do
					expect(utf8.offset(s, i)):eq(utf8.offset(s, i - l - 1, #s + 1))
				end
			end)
		end
	end)
end)
