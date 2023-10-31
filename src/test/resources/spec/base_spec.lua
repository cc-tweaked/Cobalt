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
	end)

	describe("rawequal", function()
		it("two large integers are equal", function()
			-- Older versions of Cobalt uses == instead of .equals, so this was
			-- false.
			expect(rawequal(26463, tonumber("26463"))):eq(true)
		end)
	end)

	describe("xpcall", function()
		it("accepts multiple values :lua>=5.2", function()
			local ok, res = xpcall(table.pack, function() end, 1, 2, 3)
			expect(ok):eq(true)
			expect(res):same { n = 3, 1, 2, 3 }
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
		it("loadstring uses the the thread environment", function()
			local function do_load(s) return loadstring(s) end
			setfenv(do_load, { loadstring = loadstring })
		end)

		it("can set the environment of the current thread", function()
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

		it("can set the environment of another thread", function()
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
			g = function() f(); _G.expect(_G.getfenv(1).a):eq('10') end
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
