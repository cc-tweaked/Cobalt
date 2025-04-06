describe("The string library", function()
	it("strings can be compared", function()
		assert("alo" < "alo1")
		assert("" < "a")
		assert("alo\0alo" < "alo\0b")
		assert("alo\0alo\0\0" > "alo\0alo\0")
		assert("alo" < "alo\0")
		assert("alo\0" > "alo")
		assert("\0" < "\1")
		assert("\0\0" < "\0\1")
		assert("\1\0a\0a" <= "\1\0a\0a")
		assert(not ("\1\0a\0b" <= "\1\0a\0a"))
		assert("\0\0\0" < "\0\0\0\0")
		assert(not("\0\0\0\0" < "\0\0\0"))
		assert("\0\0\0" <= "\0\0\0\0")
		assert(not("\0\0\0\0" <= "\0\0\0"))
		assert("\0\0\0" <= "\0\0\0")
		assert("\0\0\0" >= "\0\0\0")
		assert(not ("\0\0b" < "\0\0a\0"))

		-- Make sure we're using unsigned comparison.
		assert(string.char(127) >= '\0')
		assert(string.char(128) >= '\0')
		assert(string.char(127) < '\128')
	end)

	describe("string.sub", function()
		it("supports various ranges", function()
			expect(string.sub("123456789",2,4)):eq("234")
			expect(string.sub("123456789",7)):eq("789")
			expect(string.sub("123456789",7,6)):eq("")
			expect(string.sub("123456789",7,7)):eq("7")
			expect(string.sub("123456789",0,0)):eq("")
			expect(string.sub("123456789",-10,10)):eq("123456789")
			expect(string.sub("123456789",1,9)):eq("123456789")
			expect(string.sub("123456789",-10,-20)):eq("")
			expect(string.sub("123456789",-1)):eq("9")
			expect(string.sub("123456789",-4)):eq("6789")
			expect(string.sub("123456789",-6, -4)):eq("456")
			expect(string.sub("\000123456789",3,5)):eq("234")
			expect(("\000123456789"):sub(8)):eq("789")
		end)

		it("on large integers", function()
			expect(string.sub("123456789",-2^31, -4)):eq("123456")
			expect(string.sub("123456789",-2^31, 2^31 - 1)):eq("123456789")
			expect(string.sub("123456789",-2^31, -2^31)):eq("")
		end)
	end)

	describe("string.find", function()
		it("works on various ranges", function()
			expect(string.find("123456789", "345")):eq(3)
			local a,b = string.find("123456789", "345")
			expect(string.sub("123456789", a, b)):eq("345")
			expect(string.find("1234567890123456789", "345", 3)):eq(3)
			expect(string.find("1234567890123456789", "345", 4)):eq(13)
			expect(string.find("1234567890123456789", "346", 4)):eq(nil)
			expect(string.find("1234567890123456789", ".45", -9)):eq(13)
			expect(string.find("abcdefg", "\0", 5, 1)):eq(nil)
		end)

		it("works on empty strings", function()
			expect(string.find("", "")):eq(1)
			expect(string.find("", "", 1)):eq(1)
			expect(string.find('', 'aaa', 1)):eq(nil)
		end)

		it("accepts an empty match", function()
			expect({ ("foo"):find("") }):same { 1, 0 }
		end)

		it("fails on empty strings when out of bounds :lua>=5.2 :!cobalt", function()
			expect(string.find("", "", 2)):eq(nil)
			expect(("foo"):find("", 10)):eq(nil)
		end)

		it("supports the plain modifier", function()
			expect(('alo(.)alo'):find('(.)', 1, true)):eq(4)
			expect(('alo(.)alo'):find('(.)', 1, 1)):eq(4) -- Coerces to a boolean.
		end)

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

	describe("string.gsub", function()
		it("supports yielding within the replacement function :cobalt", function()
			local result, count = expect.run_coroutine(function()
				return ("hello world"):gsub("%w", function(entry)
					local x = coroutine.yield(entry)
					return x:upper()
				end)
			end)

			expect(result):eq("HELLO WORLD")
			expect(count):eq(10)
		end)

		it("back references to position captures do not error (issue #78)", function()
			string.gsub("foo", "()(%1)", "")
		end)
	end)

	describe("string.len", function()
		it("via the string library", function()
			expect(string.len("")):eq(0)
			expect(string.len("\0\0\0")):eq(3)
			expect(string.len("1234567890")):eq(10)
		end)

		it("via the operator", function()
			expect(#""):eq(0)
			expect(#"\0\0\0"):eq(3)
			expect(#"1234567890"):eq(10)
		end)
	end)

	describe("string.byte", function()
		it("byte works without a range ", function()
			expect(string.byte("a")):eq(97)
			expect(string.byte("\228")):eq(228)
		end)

		it("byte works with null bytes", function()
			expect(string.byte("\0")):eq(0)
			expect(string.byte("\0\0alo\0x", -1)):eq(string.byte('x'))
		end)

		it("works with various ranges", function()
			expect(string.byte("ba", 2)):eq(97)
			expect(string.byte("\n\n", 2, -1)):eq(10)
			expect(string.byte("\n\n", 2, 2)):eq(10)
		end)

		it("returns nil on an empty range", function()
			expect(string.byte("")):eq(nil)
			expect(string.byte("hi", -3)):eq(nil)
			expect(string.byte("hi", 3)):eq(nil)
			expect(string.byte("hi", 9, 10)):eq(nil)
			expect(string.byte("hi", 2, 1)):eq(nil)
		end)
	end)

	describe("string.char", function()
		it("returns an empty string with no args", function()
			expect(string.char()):eq("")
		end)

		it("accepts multiple characters", function()
			expect(string.char(0, 255, 0)):eq("\0\255\0")
		end)

		it("errors on out-of-range values :lua>=5.3 :!cobalt", function()
			expect.error(string.char, 256):str_match("value out of range")
			expect.error(string.char, -1):str_match("value out of range")
			expect.error(string.char, math.maxinteger or 2^40):str_match("value out of range")
			expect.error(string.char, math.mininteger or -2^40):str_match("value out of range")
		end)
	end)

	describe("string.byte/string.char round-tripping", function()
		it("single characters", function()
			expect(string.byte(string.char(255))):eq(255)
			expect(string.byte(string.char(0))):eq(0)
		end)

		it("multiple characters", function()
			expect(string.char(0, string.byte("\193"), 0)):eq("\0\193\0")
			expect(string.char(string.byte("\193l\0\243u", 1, -1))):eq("\193l\0\243u")
			expect(string.char(string.byte("\193l\0\243u", 1, 0))):eq("")
			expect(string.char(string.byte("\193l\0\243u", -10, 100))):eq("\193l\0\243u")
		end)
	end)

	it("string.upper", function()
		expect(string.upper("ab\0c")):eq("AB\0C")
	end)

	it("string.lower", function()
		expect(string.lower("\0ABCc%$")):eq("\0abcc%$")
	end)

	describe("string.rep", function()
		it("with 0 repetitions", function()
			expect(string.rep('teste', 0)):eq('')
		end)

		it("with multiple repetitions", function()
			expect(string.rep('tés\00tę', 2)):eq('tés\0tętés\000tę')
		end)

		it("with an empty string", function()
			expect(string.rep('', 10)):eq('')
		end)

		it("for various lengths", function()
			for i=0,30 do
				expect(string.len(string.rep('a', i))):eq(i)
			end
		end)

		it("with a separator :lua>=5.2", function()
			expect(string.rep('teste', 0, 'xuxu')):eq('')
			expect(string.rep('teste', 1, 'xuxu')):eq('teste')
			expect(string.rep('\1\0\1', 2, '\0\0')):eq('\1\0\1\0\0\1\0\1')
			expect(string.rep('', 10, '.')):eq(string.rep('.', 9))
		end)

		it("errors on overflows :lua>=5.3 :!cobalt", function()
			expect.error(string.rep, "aa", 2^30):eq("resulting string too large")
			expect.error(string.rep, "", 2^30, "aa"):eq("resulting string too large")
		end)
	end)

	it("string.reverse", function()
		expect(string.reverse ""):eq("")
		expect(string.reverse "\0\1\2\3"):eq("\3\2\1\0")
		expect(string.reverse "\0001234"):eq("4321\0")
	end)

	describe("string.format", function()
		it("'q' and 's' format strings", function()
			local x = '"\237lo"\n\\'
			expect(string.format('%q%s', x, x)):eq('"\\"\237lo\\"\\\n\\\\""\237lo"\n\\')
		end)

		describe("'s' option", function()
			it("with \\0", function()
				expect(string.format("%s\0 is not \0%s", 'not be', 'be')):eq('not be\0 is not \0be')
			end)

			it("converts other values to strings :lua>=5.2", function()
				expect(string.format("%s %s", nil, true)):eq("nil true")
				expect(string.format("%s %.4s", false, true)):eq("false true")
				expect(string.format("%.3s %.3s", false, true)):eq("fal tru")
				local m = setmetatable({}, {__tostring = function () return "hello" end})
				expect(string.format("%s %.10s", m, m)):eq("hello hello")
			end)

			it("uses __name :lua>=5.3", function()
				local m = setmetatable({}, { __name = "hi" })
				expect(string.format("%.4s", m, m)):eq("hi: ")
			end)

			it("errors on invalid flags :lua>=5.4", function()
				expect.error(string.format, "%0.34s", ""):str_match("invalid conversion")
				expect.error(string.format, "%0.s", ""):str_match("invalid conversion")
			end)

			it("supports yielding within __tostring :cobalt", function()
				local m = setmetatable({}, { __tostring = function()
					return coroutine.yield() .. "!"
				end })

				local co = coroutine.create(string.format)
				assert(coroutine.resume(co, "%s", m))
				local _, res = assert(coroutine.resume(co, "Hello"))
				expect(res):eq("Hello!")
			end)

			it("supports various modifiers", function()
				local x = string.format('"%-50s"', 'a')
				expect(#x):eq(52)
				expect(string.sub(x, 1, 4)):eq('"a  ')

				expect(string.format("-%.20s.20s", string.rep("%", 2000))):eq("-"..string.rep("%", 20)..".20s")
				expect(string.format('"-%20s.20s"', string.rep("%", 2000))):eq(string.format("%q", "-"..string.rep("%", 2000)..".20s"))

				expect(string.format("%.0s", "alo")):eq("")
				expect(string.format("%.s", "alo")):eq("")
			end)
		end)

		describe("'q' option", function()
			it("escapes \\0 to \\000 :lua<=5.1", function()
				expect(string.format('%q', "\0")):eq([["\000"]])
			end)

			it("escapes \\0 to \\0 :lua>=5.2 :!cobalt", function()
				expect(string.format('%q', "\0")):eq([["\0"]])
			end)

			for _, value in pairs {
				{ '"\237lo"\n\\' },
				{ "\0\1\0023\5\0009" },
				{ "\0\0\1\255" },
				-- Basic literals
				{ 2 ^ 40, nil, ":lua>=5.3" },
				{ -2 ^ 40, nil, ":lua>=5.3" },
				{ 0.1, nil, ":lua>=5.3" },
				{ true, nil, ":lua>=5.3"},
				{ nil, nil, ":lua>=5.3" },
				{ false, nil, ":lua>=5.3" },
				{ math.pi, "math.pi", ":lua>=5.3" },
				{ math.huge, "math.huge", ":lua>=5.4" },
				{ -math.huge, "math.huge", ":lua>=5.4" },
			} do
				local value, label, filter = value[1], value[2] or tostring(value[1]), value[3]
				if filter then label = label .. " " .. filter end
				it("roundtrips " .. label, function()
					local load = loadstring or load
					expect(load(string.format('return %q', value))()):eq(value)
				end)
			end

			it("roundtrips nan :lua>=5.4", function()
				local res = load(string.format('return %q', 0/0))()
				if res == res then fail(tostring(res) .. " is not nan") end
			end)

			-- Defining these separately to the above as on some systems
			-- math.mininteger is nil, and obviously that round-trips!

			it("roundtrips math.mininteger :lua>=5.3 :!cobalt", function()
				expect(math.mininteger):type("number")
				expect(load(string.format('return %q', math.mininteger))()):eq(math.mininteger)
			end)

			it("roundtrips math.maxinteger :lua>=5.3 :!cobalt", function()
				expect(math.maxinteger):type("number")
				expect(load(string.format('return %q', math.maxinteger))()):eq(math.maxinteger)
			end)

			it("errors on non-literal values :lua>=5.3", function()
				expect.error(string.format, "%q", {}):str_match("value has no literal form")
			end)

			it("formats edge-case numbers correctly :lua>=5.4", function()
				local formatted = string.format("%q %q %q", 0/0, 1/0, -1/0)
				expect(formatted):eq("(0/0) 1e9999 -1e9999")
			end)

			it("prints hexadecimal floats :lua>=5.4", function()
				expect(("%q"):format(234.53)):eq("0x1.d50f5c28f5c29p+7")
			end)

			it("errors on invalid flags :lua>=5.4", function()
				expect.error(string.format, "%10q", ""):str_match("cannot have modifiers")
				expect.error(string.format, "%#q", ""):str_match("cannot have modifiers")
			end)
		end)

		describe("'c' option", function()
			it("basic functionality", function()
				expect(string.format("\0%c\0%c%x\0", 225, string.byte("b"), 140)):eq("\0\225\0b8c\0")
				expect(
					string.format("%c",34)..string.format("%c",48)..string.format("%c",90)..string.format("%c",100)
				):eq(string.format("%c%c%c%c", 34, 48, 90, 100))
			end)

			it("supports a width", function()
				expect(string.format("%-16c", 97)):eq("a               ")
				expect(string.format("%16c", 97)):eq("               a")
			end)

			it("errors on invalid flags :lua>=5.4", function()
				expect.error(string.format, "%010c", 10):str_match("invalid conversion")
				expect.error(string.format, "%.10c", 10):str_match("invalid conversion")
			end)
		end)

		describe("'d' option", function()
			it("basic modifiers", function()
				expect(string.format("%%%d %010d", 10, 23)):eq("%10 0000000023")
				expect(string.format("%+08d", 2^31 - 1)):eq("+2147483647")
				expect(string.format("%+08d", -2^31)):eq("-2147483648")
			end)

			it("supports very large numbers", function()
				expect(string.format("%d", -1)):eq("-1")
				expect(tonumber(string.format("%u", 2^62))):eq(2^62)
				expect(string.format("%d", 2^53)):eq("9007199254740992")
				expect(string.format("%d", -2^53)):eq("-9007199254740992")
			end)

			it("errors on repeated flags :lua<=5.3", function()
				expect.error(string.format, "%0000000000d", 10):eq("invalid format (repeated flags)")
			end)

			it("errors on invalid options :lua>=5.4", function()
				expect.error(string.format, "%#i", 0):str_match("invalid conversion")
			end)

			describe("non-integer numbers", function()
				it("allows all values :lua==5.1 :!cobalt", function()
					expect(string.format("%d", 1.2)):eq("1")
					expect(string.format("%d", 0/0)):eq("-9223372036854775808")
					expect(string.format("%d", math.huge)):eq("-9223372036854775808")
				end)

				it("allows real numbers :lua==5.2", function()
					expect(string.format("%d", 1.2)):eq("1")
					expect.error(string.format, "%d", 0/0):str_match("not a number in proper range")
					expect.error(string.format, "%d", math.huge):str_match("not a number in proper range")
				end)

				it("errors :lua>=5.3 :!cobalt", function()
					expect.error(string.format, "%d", 1.2):str_match("number has no integer representation")
					expect.error(string.format, "%d", 0/0):str_match("number has no integer representation")
					expect.error(string.format, "%d", math.huge):str_match("number has no integer representation")
				end)
			end)

			it("supports various modifiers", function()
				expect(string.format("%#12o", 10)):eq("         012")
				expect(string.format("%013i", -100)):eq("-000000000100")
				expect(string.format("%2.5d", -100)):eq("-00100")
				expect(string.format("%.u", 0)):eq("")
			end)
		end)

		describe("'f' option", function()
			it("round trips", function()
				expect(tonumber(string.format("%f", 10.3))):eq(10.3)
			end)

			it("with large widths", function()
				local res = string.format('%99.99f', -1e308)
				if #res < 101 + 38 then fail("String is " .. #res) end
				expect(tonumber(res)):eq(-1e308)
			end)

			it("errors on too large ranges", function()
				expect.error(string.format, "%1"..("0"):rep(600)..".3d", 10):str_match("too long")

				local msg =  _VERSION == "Lua 5.4" and "invalid conversion" or "too long"
				expect.error(string.format, "%100.3d", 10):str_match(msg)
				expect.error(string.format, "%1.100d", 10):str_match(msg)

				expect.error(string.format, "%10.1"..("0"):rep(600).."004d", 10):str_match("too long")
			end)

			it("supports various modifiers", function()
				expect(string.format("%+#014.0f", 100)):eq("+000000000100.")
				expect(string.format("% 1.0E", 100)):eq(" 1E+02")
				expect(string.format("%+.3G", 1.5)):eq("+1.5")
				expect(string.format("% .1g", 2^10)):eq(" 1e+03")
			end)
		end)

		describe("'x' option", function()
			it("on floats :lua<=5.2", function()
				expect(string.format("%x", 0.3)):eq("0")
				expect(string.format("%02x", 0.1)):eq("00")
			end)

			it("with large numbers", function()
				expect(string.format("%08X", 2^32 - 1)):eq("FFFFFFFF")

				expect(string.format("%8x", 2^52 - 1)):eq("fffffffffffff")
				expect(string.format("%8x", 0xffffffff)):eq("ffffffff")
				expect(string.format("%8x", 0x7fffffff)):eq("7fffffff")
				expect(string.format("0x%8X", 0x8f000003)):eq("0x8F000003")
			end)

			it("supports formatting flags", function()
				expect(string.format("%#10x", 100)):eq("      0x64")
				expect(string.format("%#-17X", 100)):eq("0X64             ")
			end)

			it("negative numbers :lua~=5.2", function()
				expect(string.format("%x", -1)):eq("ffffffffffffffff")
			end)

			it("with very large numbers :lua==5.1 :!cobalt", function()
				expect(string.format("%x", 5e19)):eq("0")
			end)

			it("with very large numbers :lua==5.2", function()
				expect.error(string.format, "%x", 5e19):str_match("number in proper range")
			end)

			it("with very large numbers :lua>=5.3 :!cobalt", function()
				expect.error(string.format, "%x", 5e19):str_match("number has no integer representation")
			end)
		end)

		describe("'a' option :lua>=5.2", function()
			it("basic functionality", function()
				expect(string.format("%.2a", 0.5)):eq("0x1.00p-1")
				expect(string.format("%A", 0x1fffffffffffff)):eq("0X1.FFFFFFFFFFFFFP+52")
				expect(string.format("%.4a", -3)):eq("-0x1.8000p+1")
				expect(tonumber(string.format("%a", -0.1))):eq(-0.1)
			end)

			it("zero", function()
				expect(string.format("%A", 0.0)):eq("0X0P+0")
			end)

			-- Broken on Cobalt as it converts "0.0" to an integer, so when we
			-- take the negative, it stays as 0.
			it("negative zero :!cobalt", function()
				expect(string.format("%a", -0.0)):eq("-0x0p+0")
			end)

			it("weird numbers", function()
				expect(string.format("%a", 1/0)):eq("inf")
				expect(string.format("%A", -1/0)):eq("-INF")
				expect(string.format("%a", 0/0)):str_match("^%-?nan$")
			end)
		end)

		it("very large numbers :lua<=5.2", function()
			local x = 2^64 - 2^(64-53)
			expect(x):eq(0xfffffffffffff800)
			expect(tonumber(string.format("%u", x))):eq(x)
			expect(tonumber(string.format("0X%x", x))):eq(x)
			expect(string.format("%x", x)):eq("fffffffffffff800")
			expect(string.format("%d", x/2)):eq("9223372036854774784")
			expect(string.format("%d", -x/2)):eq("-9223372036854774784")
			expect(string.format("%d", -2^63)):eq("-9223372036854775808")
			expect(string.format("%x", 2^63)):eq("8000000000000000")
		end)

		it("very large numbers :lua>=5.3 :!cobalt", function()
			local max, min = 0x7fffffffffffffff, -0x8000000000000000
			expect(string.format("%x", 0xfffffffffffff)):eq("fffffffffffff")
			expect(string.format("0x%8X", 0x8f000003)):eq("0x8F000003")
			expect(string.format("%d", 2^53)):eq("9007199254740992")
			expect(string.format("%i", -2^53)):eq("-9007199254740992")
			expect(string.format("%x", max)):eq("7fffffffffffffff")
			expect(string.format("%x", min)):eq("8000000000000000")
			expect(string.format("%d", max)):eq("9223372036854775807")
			expect(string.format("%d", min)):eq("-9223372036854775808")
			expect(string.format("%u", -1)):eq("18446744073709551615")
			expect(tostring(1234567890123)):eq('1234567890123')
		end)

		it("errors on invalid options :lua>=5.4", function()
			-- The error message was different on previous versions - Cobalt uses the 5.4 version
			expect.error(string.format, "%t", 10):eq("invalid conversion '%t' to 'format'")
		end)
	end)

	describe("string.pack", function()
		it("'z' modifier on exactly the buffer boundary :lua>=5.3", function()
			local packed = string.pack("z", ("#"):rep(32))
			expect(packed):eq("################################\0")
		end)
	end)
end)
