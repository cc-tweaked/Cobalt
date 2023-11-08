describe("Varargs", function()
	local load = loadstring or load
	local unpack = table.unpack or unpack
	local function pack(...) return { n = select('#', ...), ... } end

	local function call(f, args) return f(unpack(args, 1, args.n)) end

	describe("Lua 5.0 varargs :lua<=5.1 :!cobalt", function()
		local function check(a, ...)
			expect(arg):same(a)
			return arg.n
		end

		it("no arguments", function() check({ n = 0}) end)
		it("some arguments", function() check({n = 3, 1, 2, 3}, 1, 2, 3) end)
		it("with holes", function() check({ n = 5, "alo", nil, 45, f, nil}, "alo", nil, 45, f, nil) end)

		it("is nil when ... is used", function()
			(function(...)
				expect(arg):eq(nil)
				return ...
			end)(1, 2, 3)
		end)
	end)

	it("No Lua 5.0 varargs :lua>=5.2", function()
		(function(...)
			expect(arg):eq(_G.arg)
			return ...
		end)(1, 2, 3)
	end)

	describe("for main chunks", function()
		it("basic packing", function()
			local func = assert(load [[ return { ... } ]])
			expect(func(2, 3)):same { 2, 3 }
		end)

		it("with select('#')", function()
			local func = assert(load [[
				local x = {...}
				for i = 1, select('#', ...) do expect(x[i]):eq(select(i, ...)) end
				expect(x[select('#', ...) + 1]):eq(nil)
			]])

			func("a", "b", nil)
			func()
		end)
	end)

	describe("select", function()
		it("selects multiple values", function()
			expect({select(3, unpack{10,20,30,40})}):same { 30, 40 }
		end)

		it("returns nothing when out of bounds", function()
			expect({select(1)}):same {}
		end)

		it("accepts negative indexes", function()
			expect({ select(-1, 3, 5, 7) }):same { 7 }
			expect({ select(-2, 3, 5, 7) }):same { 5, 7 }
		end)

		it("errors when out of bounds", function()
			expect.error(select, -10000):str_match("index out of range")
		end)
	end)

	it("allows unpacking a large number of values :lua>=5.2", function()
		local len = 2 ^ 19
		local tbl = { (" "):rep(len):byte(1, -1) }
		expect(#tbl):eq(len)
	end)

	it("supports consing and unconsing", function()
		local function drop(a, ...) return ... end

		local function f(n, a, ...)
			local b
			if n == 0 then
				local b, c, d = ...
				return a, b, c, d, drop(drop(drop(...)))
			else
				n, b, a = n - 1, ..., a
				expect(b):eq((...))
				return f(n, a, ...)
			end
		end

		local a, b, c, d, e = assert(f(10, 5, 4, 3, 2, 1))
		expect({ a, b, c, d, e }):same { 5, 4, 3, 2, 1 }

		local a, b, c, d, e = f(4)
		expect({ a, b, c, d, e }):same { nil, nil, nil, nil, nil }
	end)

	it("calling self functions with varargs", function()
		local t = {1, 10}
		function t:f(...) return self[...] + select('#', ...) end
		expect(t:f(1, 4)):eq(3)
		expect(t:f(2)):eq(11)
	end)

	it("calling mixed args/varargs with varargs", function()
		local lim = 20
		local a = {}
		for i = 1, 20 do a[i] = i+0.3 end

		local function f(a, b, c, d, ...)
			local more = {...}
			expect(a):eq(1.3)
			expect(more[1]):eq(5.3)
			expect(more[lim - 4]):eq(lim + 0.3)
			expect(more[lim - 3]):eq(nil)
		end

		call(f, a)
	end)

	it("calling fixed args with varargs", function()
		local lim = 20
		local a = {}
		for i = 1, 20 do a[i] = i+0.3 end

		local function g(a,b,c)
			expect(a):eq(1.3)
			expect(b):eq(2.3)
			expect(c):eq(3.3)
		end

		call(g, a)
	end)

	it("calling C function with varargs", function()
		local a = {}
		for i = 1, 20 do a[i] = i end
		expect(call(math.max, a)):eq(20)
	end)

	it("chaining of varargs", function()
		local a = pack(call(next, {_G,nil;n=2}))
		local b,c = next(_G)
		expect(a):same { n = 2, b, c }
	end)

	describe("PUC Lua tests", function()
		-- These are dubiously useful, for various reasons:
		--  - Not clear what they're trying to test.
		--  - Describe bugs which only error when Lua was compiled with
		--    assertions

		it("misc tests", function()
			local function c12 (...)
				local x = {...}; x.n = #x
				local res = (x.n==2 and x[1] == 1 and x[2] == 2)
				if res then res = 55 end
				return res, 2
			end

			expect(c12(1,2)):eq(55)
			local a,b = assert(call(c12, {1,2}))
			expect(a):eq(55) expect(b):eq(2)

			local a = call(c12, {1,2;n=2})
			expect(a):eq(55) expect(b):eq(2)

			local a = call(c12, {1,2;n=1})
			expect(a):eq(false)

			expect(c12(1,2,3)):eq(false)

			local a = pack(call(call, {c12, {1,2}}))
			expect(a):same { n = 2, 55, 2 }
		end)

		it("no stack overflow with many parameters", function()
			-- From the Lua 5.3 test suite - see https://www.lua.org/bugs.html#5.2.2
			local function f(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10,
				p11, p12, p13, p14, p15, p16, p17, p18, p19, p20,
				p21, p22, p23, p24, p25, p26, p27, p28, p29, p30,
				p31, p32, p33, p34, p35, p36, p37, p38, p39, p40,
				p41, p42, p43, p44, p45, p46, p48, p49, p50, ...)
				local a1,a2,a3,a4,a5,a6,a7
				local a8,a9,a10,a11,a12,a13,a14
			end

			-- assertion fail here
			f()
		end)

		it("missing arguments in tail call", function()
			-- From the Lua 5.4 test suite. I'm assuming there was a bug where
			-- the stack wasn't adjusted, but its not clear to me.
			local function f(a,b,c) return c, b end
			local function g() return f(1,2) end
			local a, b = g()
			expect(a):eq(nil)
			expect(b):eq(2)
		end)
	end)
end)
