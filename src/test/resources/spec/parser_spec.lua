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

	describe("locals", function()
		it("are lexically scoped", function()
			local i = 10
			do local i = 100; expect(i):eq(100) end
			do local i = 1000; expect(i):eq(1000) end
			expect(i):eq(10)
			if i ~= 10 then
				local i = 20
			else
				local i = 30
				expect(i):eq(30)
			end
		end)

		it("environments are lexically scoped :lua>=5.2", function()
			local function getenv(f)
				local a,b = debug.getupvalue(f, 1)
				expect(a):eq("_ENV")
				return b
			end

			expect(_ENV):eq(_G)

			local _ENV = (function (...) return ... end)(_G, dummy)

			do local _ENV = { assert = assert } assert(true) end
			mt = { _G = _G }
			local foo, x
			do
				local _ENV = mt
				function foo(x)
					A = x
					do local _ENV = _G; A = 1000 end
					return function (x) return A .. x end
				end
			end
			expect(getenv(foo)):eq(mt)
			x = foo('hi')
			expect(mt.A):eq('hi')
			expect(A):eq(1000)

			expect(x('*')):eq(mt.A .. '*')

			do
				local _ENV = { expect = expect, A = 10};
				do
					local _ENV = { expect = expect, A = 20}
					expect(A):eq(20)
					x = A
				end
				expect(A):eq(10) expect(x):eq(20)
			end

			expect(x):eq(20)
		end)
	end)

	describe("parse errors", function()
		local function it_error(name, code, msg)
			it(name, function()
				local fn, err = (loadstring or load)(code)
				expect(fn):eq(nil)
				if msg then expect(err):str_match(msg) end
			end)
		end

		local function it_lua51(name, code, msg)
			it_error(name .. " :lua==5.1 :!cobalt", code, msg)
			it(name .. ":lua~=5.1", function()
				local fn, err = load(code)
				if not fn then fail(err) end
			end)
		end

		it_error("break with no scope", "break label()", "loop")
		it_lua51("bare semicolon", ";")
		it_lua51("multiple semicolons", "a=1;;")
		it_error("multiple semicolons after return", "return;;")
		it_lua51("ambiguous call syntax", "a=math.sin\n(3)")
		it_error("bare variable after repeat", "repeat until 1; a")
		it_error("multiple varargs", "function a (... , ...) end")
		it_error("comma after varargs", "function a (, ...) end")
	end)
end)
