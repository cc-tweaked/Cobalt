describe("The Lua VM", function()
	describe("type errors", function()
		local function mk_name(name) return setmetatable({}, {__name = name}) end

		it("uses __name in comparisons :lua>=5.3", function()
			local a, b = mk_name "type a", mk_name "type b"
			expect.error(function() return a < b end):str_match("attempt to compare type a with type b$")
			expect.error(function() return a < a end):str_match("attempt to compare two type a values$")
		end)

		it("uses __name in argument errors :lua>=5.3", function()
			local a = mk_name "type a"
			expect.error(string.match, a):str_match("string expected, got type a")
		end)
	end)

	it("can yield within metamethods :lua>=5.2", function()
		local create, ops
		create = function(val) return setmetatable({ x = val }, ops) end
		ops = {
			__add = function(x, y)
				local a, b = coroutine.yield(x, y)
				return create(a.x + b.x)
			end,
			__div = function(x, y)
				local a, b = coroutine.yield(x, y)
				return create(a.x / b.x)
			end,
			__concat = function(x, y)
				local a, b = coroutine.yield(x, y)
				return create(a.x .. b.x)
			end,
			__eq = function(x, y)
				local a, b = coroutine.yield(x, y)
				return a.x == b.x
			end,
			__lt = function(x, y)
				local a, b = coroutine.yield(x, y)
				return a.x < b.x
			end,
			__len = function(x)
				return coroutine.yield(x).x
			end,
			__index = function(tbl, key)
				local res = coroutine.yield(key)
				return res:upper()
			end,
			__newindex = function(tbl, key, val)
				local rKey, rVal = coroutine.yield(key, val)
				rawset(tbl, rKey, rVal .. "!")
			end,
		}

		local varA = create(2)
		local varB = create(3)

		expect.run_coroutine(function()
			expect(5):eq((varA + varB).x)
			expect(5):eq((varB + varA).x)
			expect(4):eq((varA + varA).x)
			expect(6):eq((varB + varB).x)

			expect(2 / 3):eq((varA / varB).x)
			expect(3 / 2):eq((varB / varA).x)
			expect(1):eq((varA / varA).x)
			expect(1):eq((varB / varB).x)

			expect("23"):eq((varA .. varB).x)
			expect("32"):eq((varB .. varA).x)
			expect("22"):eq((varA .. varA).x)
			expect("33"):eq((varB .. varB).x)
			expect("33333"):eq((varB .. varB .. varB .. varB .. varB).x)

			expect(false):eq(varA == varB)
			expect(false):eq(varB == varA)
			expect(true):eq(varA == varA)
			expect(true):eq(varB == varB)

			expect(true):eq(varA < varB)
			expect(false):eq(varB < varA)
			expect(false):eq(varA < varA)
			expect(false):eq(varB < varB)

			expect(true):eq(varA <= varB)
			expect(false):eq(varB <= varA)
			expect(true):eq(varA <= varA)
			expect(true):eq(varB <= varB)

			expect(#varA):eq(2)

			expect("HELLO"):eq(varA.hello)
			varA.hello = "bar"
			expect("bar!"):eq(varA.hello)
		end)
	end)

	describe("error positions", function()
		it("includes positions when there is a single frame", function()
			local function f() string.gsub(nil) end
			local ok, err = coroutine.resume(coroutine.create(f))

			local info = debug.getinfo(f, "S")
			local prefix = info.short_src .. ":" .. info.linedefined .. ": bad argument"
			expect(ok):eq(false)
			expect(err:sub(1, #prefix)):eq(prefix)
		end)
	end)
end)
