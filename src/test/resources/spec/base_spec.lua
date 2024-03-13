describe("The base library", function()
	describe("assert", function()
		it("returns values by default", function()
			expect({assert("a", true, 3)}):same { "a", true, 3 }
		end)

		it("requires at least one argument :lua~=5.2", function()
			expect.error(assert):str_match("bad argument #1"):str_match("value expected")
		end)

		it("defaults to 'assertion failed!'", function()
			expect.error(assert, false):eq("assertion failed!")
		end)

		it("accepts any type as an error message :lua>=5.3", function()
			expect.error(assert, false, 123):eq(123)
		end)
	end)

	describe("tonumber", function()
		it("rejects partial numbers", function()
			local invalid = { "-", " -", "- ", " - ", "0x" }
			for _, k in pairs(invalid) do
				expect(tonumber(k)):describe(("tonumber(%q)"):format(k)):eq(nil)
			end
		end)
	end)

	describe("tostring", function()
		it("nil", function() expect(tostring(nil)):eq("nil") end)

		it("booleans", function()
			expect(tostring(true)):eq("true")
			expect(tostring(false)):eq("false")
		end)

		describe("numbers", function()
			it("basic conversions", function()
				expect(tostring(12)):eq("12")
				expect(""..12):eq('12')
				expect(12 .. ""):eq('12')

				expect(tostring(1234567890123)):eq("1234567890123")
			end)

			it("floats", function()
				expect(tostring(-1203)):eq("-1203")
				expect(tostring(1203.125)):eq("1203.125")
				expect(tostring(-0.5)):eq("-0.5")
				expect(tostring(-32767)):eq("-32767")
			end)

			it("integers :lua>=5.3", function()
				expect(tostring(4611686018427387904)):eq("4611686018427387904")
				expect(tostring(-4611686018427387904)):eq("-4611686018427387904")
			end)

			it("integer-compatible floats are preserved :lua>=5.3 :!cobalt", function()
				expect('' .. 12):eq('12') expect(12.0 .. ''):eq('12.0')
				expect(tostring(-1203 + 0.0)):eq("-1203.0")
			end)

			it("integer-compatible floats are truncated :lua<=5.2", function()
				expect(tostring(0.0)):eq("0")
				expect('' .. 12):eq('12') expect(12.0 .. ''):eq('12')
				expect(tostring(-1203 + 0.0)):eq("-1203")
			end)
		end)

		it("tables", function() expect(tostring {}):str_match('^table:') end)

		it("functions", function() expect(tostring(print)):str_match('^function:') end)

		it("null bytes", function()
			expect(tostring('\0')):eq("\0")
		end)

		it("uses __name :lua>=5.3", function()
			local obj = setmetatable({}, { __name = "abc" })
			expect(tostring(obj)):str_match("^abc: ")
		end)

		it("errors if __tostring does not return a string :lua>=5.3 :!cobalt", function()
			local obj = setmetatable({}, { __tostring = function () return {} end })
			expect.error(tostring, obj):eq("'__tostring' must return a string")
		end)

		it("can return a non-string value :lua<=5.2", function()
			-- Lua 5.3+ requires this to be a string. Which is sensible, but a breaking change!
			local obj = setmetatable({}, { __tostring = function() return false end })
			expect(tostring(obj)):eq(false)
		end)

		it("supports yielding in __tostring :cobalt", function()
			local obj = setmetatable({}, {
				__tostring = function() return coroutine.yield("hello") .. "!", "extra" end
			})
			local res, extra = expect.run_coroutine(function() return tostring(obj) end)

			expect(res):eq("hello!")
			expect(extra):eq(nil)
		end)
	end)

	describe("pairs", function()
		it("supports yielding in _pairs :cobalt", function()
			local function custom_next(self, k)
				local k, v = next(self, k)
				if k == nil then return nil else return k, v .. "!" end
			end
			local obj = setmetatable({ "a", "b", "c" }, {
				__pairs = function(self) return coroutine.yield(custom_next), self end
			})
			expect.run_coroutine(function()
				local n = 0
				for i, x in pairs(obj) do
					n = n + 1
					expect(x):eq(obj[i] .. "!")
				end

				expect(n):eq(3)
			end)
		end)
	end)

	describe("ipairs", function()
		local function make_slice(tbl, start, len)
			return setmetatable({}, {
				__index = function(self, i)
					if i >= 1 and i <= len then return tbl[start + i - 1] end
				end,
			})
		end

		it("inext returns nil when nothing left :lua>=5.2", function()
			local inext = ipairs({})
			expect(select('#', inext({}, 0))):eq(1)
		end)

		it("uses metamethods :lua>=5.3", function()
			local slice = make_slice({ "a", "b", "c", "d", "e" }, 2, 3)
			local inext = ipairs(slice)

			expect({ inext(slice, 0) }):same { 1, "b" }
			expect({ inext(slice, 1) }):same { 2, "c" }
			expect({ inext(slice, 2) }):same { 3, "d" }
			expect({ inext(slice, 3) }):same { nil }
		end)

		it("uses metamethods on non-table values :lua>=5.3", function()
			local inext = ipairs("hello")
			expect(inext("hello", 0)):eq(nil)
		end)

		it("supports yielding in __index :cobalt", function()
			local base = { "a", "b", "c" }
			local obj = setmetatable({}, {
				__index = function(self, i)
					local item = coroutine.yield(base[i])
					if item == nil then return nil else return item .. "!" end
				end
			})
			expect.run_coroutine(function()
				local n = 0
				for i, x in ipairs(obj) do
					n = n + 1
					expect(x):eq(base[i] .. "!")
				end

				expect(n):eq(3)
			end)
		end)
	end)

	describe("rawequal", function()
		it("two large integers are equal", function()
			-- Older versions of Cobalt uses == instead of .equals, so this was
			-- false.
			expect(rawequal(26463, tonumber("26463"))):eq(true)
		end)
	end)

	describe("load", function()
		it("returns the error value", function()
			local tbl = {}
			local fun, err = load(function() error(tbl) end)

			expect(fun):eq(nil)
			expect(err):eq(tbl)
		end)

		local getenv = getfenv or function(f)
			local a,b = debug.getupvalue(f, 1)
			expect(a):eq("_ENV")
			return b
		end

		it("defaults to the global environment :lua>=5.2", function()
			expect(getenv(load"a = 3")):eq(_G)
		end)

		it("accepts custom environments :lua>=5.2", function()
			local c = {}
			local f = load("a = 3", nil, nil, c)
			expect(getenv(f)):eq(c)
			expect(c.a):eq(nil)
			f()
			expect(c.a):eq(3)
		end)

		-- I'd hope nobody relies on this behaviour, but you never know!
		it("propagates the current error handler", function()
			local res = {
				xpcall(
					function() return load(function() error("oh no", 0) end) end,
					function(e) return "caught " .. e end
				)
			}
			expect(res):same { true, nil, "caught oh no"}
		end)
	end)

	describe("pcall", function()
		describe("supports yielding :lua>=5.2", function()
			it("with no error", function()
				local ok, a, b, c = expect.run_coroutine(function()
					return pcall(function()
						local a, b, c = coroutine.yield(1, 2, 3)
						return a, b, c
					end)
				end)

				expect(ok):eq(true)
				expect({ a, b, c }):same { 1, 2, 3 }
			end)

			it("with an error", function()
				local ok, msg = expect.run_coroutine(function()
					return pcall(function()
						local a, b, c = coroutine.yield(1, 2, 3)
						expect({ a, b, c }):same { 1, 2, 3 }
						error("Error message", 0)
					end)
				end)

				expect(ok):eq(false)
				expect(msg):eq("Error message")
			end)
		end)
	end)

	describe("xpcall", function()
		it("accepts multiple values :lua>=5.2", function()
			local ok, res = xpcall(table.pack, function() end, 1, 2, 3)
			expect(ok):eq(true)
			expect(res):same { n = 3, 1, 2, 3 }
		end)

		local function overflow()
			-- Create a load of useless locals to ensure we fill the stack on PUC Lua faster.
			local a1, b1, c1, d1, e1, f1, g1, h1, i1, j1, k1, l1, m1, n1, o1, p1, q1, r1, s1, t1, u1, v1, w1, x1, y1, z1
			local a2, b2, c2, d2, e2, f2, g2, h2, i2, j2, k2, l2, m2, n2, o2, p2, q2, r2, s2, t2, u2, v2, w2, x2, y2, z2
			local a3, b3, c3, d3, e3, f3, g3, h3, i3, j3, k3, l3, m3, n3, o3, p3, q3, r3, s3, t3, u3, v3, w3, x3, y3, z3
			local a4, b4, c4, d4, e4, f4, g4, h4, i4, j4, k4, l4, m4, n4, o4, p4, q4, r4, s4, t4, u4, v4, w4, x4, y4, z4
			local a5, b5, c5, d5, e5, f5, g5, h5, i5, j5, k5, l5, m5, n5, o5, p5, q5, r5, s5, t5, u5, v5, w5, x5, y5, z5
			local a6, b6, c6, d6, e6, f6, g6, h6, i6, j6, k6, l6, m6, n6, o6, p6, q6, r6, s6, t6, u6, v6, w6, x6, y6, z6
			local a7, b7, c7, d7, e7, f7, g7, h7, i7, j7, k7, l7, m7, n7, o7, p7, q7, r7, s7, t7, u7, v7, w7, x7, y7, z7
			overflow()
		end

		describe("handles stack overflows", function()
			-- PUC Lua and Cobalt handle stacks rather differently. For PUC Lua, the stack holds all Lua values on it.
			-- For Cobalt, the stack just holds the current call frames - each call frame then gets its own array for
			-- storing registers.
			--
			-- This means that the idea of a stack overflow is a little different. That said, the approach used to
			-- handle and recover from stack overflows are similar - we grow the stack to a new limit, and then shrink
			-- it again afterwards.

			it("allows a basic handler", function()
				local ok, res = xpcall(overflow, debug.traceback)
				expect(ok):eq(false)
				expect(res):str_match(": stack overflow\nstack traceback:")
			end)

			it("the handler can call a small number of nested functions", function()
				local ok, res = xpcall(overflow, function(err)
					local contains_overflow = err:find(": stack overflow$")
					return { err, contains_overflow }
				end)

				expect(ok):eq(false)
				expect(res[2]):type("number")
			end)

			it("overflowing the stack doesn't allow further overflows", function()
				-- Ensure the stack is properly shrunk
				local ok, res = xpcall(overflow, function(err) return err end)
				expect(ok):eq(false)
				expect(res):str_match("stack overflow$")

				local ok, res = xpcall(overflow, function(err) return err end)
				expect(ok):eq(false)
				expect(res):str_match("stack overflow$")
			end)

			it("we can nest error handlers", function()
				-- Ensure we don't prematurely shrink the stack.
				local ok, res = xpcall(overflow, function(err)
					local ok, err = xpcall(function() error("An error occurred", 0) end, function(e) return e end)
					return { ok, err }
				end)
				expect(ok):eq(false)
				expect(res):same { false, "An error occurred" }
			end)
		end)

		describe("supports yielding :lua>=5.2", function()
			it("within the main function", function()
				-- Ensure that yielding within a xpcall works as expected
				expect.run_coroutine(function()
					local ok, a, b, c = xpcall(function()
						return coroutine.yield(1, 2, 3)
					end, function(msg) return msg .. "!" end)

					expect(true):eq(ok)
					expect(1):eq(a)
					expect(2):eq(b)
					expect(3):eq(c)
				end)
			end)

			it("within the main function (with an error)", function()
				expect.run_coroutine(function()
					local ok, msg = xpcall(function()
						local a, b, c = coroutine.yield(1, 2, 3)
						expect(1):eq(a)
						expect(2):eq(b)
						expect(3):eq(c)

						error("Error message", 0)
					end, function(msg) return msg .. "!" end)

					expect(false):eq(ok)
					expect("Error message!"):eq(msg)
				end)
			end)

			it("with an error in the error handler", function()
				expect.run_coroutine(function()
					local ok, msg = xpcall(function()
						local a, b, c = coroutine.yield(1, 2, 3)
						expect(1):eq(a)
						expect(2):eq(b)
						expect(3):eq(c)

						error("Error message")
					end, function(msg) error(msg) end)

					expect(false):eq(ok)
					expect("error in error handling"):eq(msg)
				end)
			end)

			it("within the error handler :cobalt", function()
				expect.run_coroutine(function()
					local ok, msg = xpcall(function()
						local a, b, c = coroutine.yield(1, 2, 3)
						expect(1):eq(a)
						expect(2):eq(b)
						expect(3):eq(c)

						error("Error message", 0)
					end, function(msg)
						return coroutine.yield(msg) .. "!"
					end)

					expect(false):eq(ok)
					expect("Error message!"):eq(msg)
				end)
			end)

			it("within the error handler with an error :cobalt", function()
				expect.run_coroutine(function()
					local yielded = false
					local ok, msg = xpcall(function()
						local a, b, c = coroutine.yield(1, 2, 3)
						expect(1):eq(a)
						expect(2):eq(b)
						expect(3):eq(c)

						error("Error message", 0)
					end, function(msg)
						coroutine.yield(msg)
						yielded = true
						error("nope")
					end)

					expect(false):eq(ok)
					expect("error in error handling"):eq(msg)
					expect(yielded):describe("Yielded"):eq(true)
				end)
			end)
		end)
	end)

	describe("getfenv/setfenv :lua==5.1", function()
		it("can set the environment of the current thread :!cobalt", function()
			local finished = false
			local f = coroutine.wrap(function(env)
				setfenv(0, env)
				coroutine.yield(getfenv())
				expect(getfenv(0)):describe("Thread environment has changed."):eq(env)
				expect(getfenv(1)):describe("Our environment is the same."):eq(_G)
				expect(getfenv(loadstring"")):describe("New environments are the same."):eq(env)
				finished = true
				return getfenv()
			end)

			local a = {}
			expect(f(a)):eq(_G)
			expect(f()):eq(_G)
		end)

		it("can set the environment of another thread :!cobalt", function()
			local co = coroutine.create(function ()
				coroutine.yield(getfenv(0))
				return loadstring("return a")()
		  	end)

			local a = {a = 15}
			debug.setfenv(co, a)
			expect(debug.getfenv(co)):eq(a)
			expect(select(2, coroutine.resume(co))):eq(a)
			expect(select(2, coroutine.resume(co))):eq(a.a)
		end)

		it("can set the environment of closures", function()
			local _G = _G
			local g
			local function f () expect(setfenv(2, {a='10'})):eq(g) end
			g = function()
				local _ = some_global -- Force us to get an _ENV upvalue
				f(); _G.expect(_G.getfenv(1).a):eq('10')
			end
			g();
			expect(getfenv(g).a):eq('10')
		end)

		it("more complex closure usage", function()
			local _G = _G

			-- Create a bunch of functions which increment a (global) counter.
			local f = {}
			for i=1,10 do f[i] = function(x) A=A+1; return A, _G.getfenv(x) end end

			A = 10 -- Intentionally non-local
			expect(f[1]()):eq(11)

			-- Now put all our functions in their own environment
			for i=1,10 do expect(setfenv(f[i], {A=i})):eq(f[i]) end

			-- Calling f[3] should just increment its and do nothing else
			expect(f[3]()):eq(4)
			expect(A):eq(11)

			-- Passing 1 to get the function's environment
			local a, b = f[8](1)
			expect(b.A):eq(9)

			-- Passing 0 to get the thread environment.
			a,b = f[8](0)
			expect(b.A):eq(11)
		end)
	end)
end)
