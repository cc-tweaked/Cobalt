describe("Lua tables", function()
	local maxI, minI
	-- Not clear what the definition of maxinteger is on Cobalt, so for now we
	-- just assume it matches Java's version.
	if math.maxinteger then
		maxI, minI = math.maxinteger, math.mininteger
	else
		maxI, minI = 2^31 - 1, -2^31
	end

	-- Create a slice of a table - the returned table is a view of the original contents, not a copy.
	--
	-- This is mostly intended for testing functions which use metamethods.
	local function make_slice(tbl, start, len)
		return setmetatable({}, {
			__len = function() return len end,
			__index = function(self, i)
				if i >= 1 and i <= len then return tbl[start + i - 1] end
			end,
			__newindex = function(self, i, x)
				if i < 1 or i > len then error("index out of bounds", 2) end
				tbl[start + i - 1] = x
			end,
		})
	end

	-- Count the number of items in a table
	local function size(t)
		local n = 0
		for _ in pairs(t) do n = n + 1 end
		return n
	end

	describe("can have keys set", function()
		describe("invalid keys", function()
			it("are rejected", function()
				local t = {}
				expect.error(function() t[nil] = true end):str_match("table index is nil$")
				expect.error(function() t[0/0] = true end):str_match("table index is NaN$")
			end)

			it("are allowed on tables with __newindex :lua>=5.2", function()
				local t = setmetatable({}, {__newindex = function() end})
				t[nil] = true
				t[0/0] = true
			end)
		end)
	end)

	describe("have a length operator", function()
		it("behaves identically to PUC Lua on sparse tables", function()
			-- Ensure the length operator on sparse tables behaves identically to PUC Lua.
			expect(#{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, nil, 17, 18, [33] = {} }):eq(18)
			expect(#{ 1, 2, 3, nil, 5, nil, nil, 8, 9 }):eq(9)
			expect(#{ 1, 2, 3, nil, 5, nil, 7, 8 }):eq(8)
			expect(#{ 1, 2, 3, nil, 5, nil, 7, 8, 9 }):eq(9)
			expect(#{ 1, nil, [2] = 2, 3 }):eq(3)
		end)

		it("behaves identically to PUC Lua after resizing", function()
			local n = 8

			-- We grow the array part to be of length N.
			local tbl = {}
			for i = 1, n do tbl[i] = true end
			expect(#tbl):eq(n)

			-- Then clear out all but the last value. This does not shrink the array part, so we
			-- still have a length N.
			for i = 1, n - 1 do tbl[i] = nil end
			expect(#tbl):eq(n)
		end)
	end)

	describe("can be constructed from varargs", function()
		it("presizes the array", function()
			local function create(...) return { ... } end

			-- If we'd constructed this table normally, it'd have a length of 5. However,
			-- SETLIST will presize the table to ensure the array part is of length 5. As
			-- the last slot is full, the array is considered saturated.
			expect(#create(nil, nil, nil, nil, 1)):eq(5)
		end)
	end)

	describe("weak tables", function()
		local function setmode(t, mode)
			-- Note we have to create a new metatable here, as Cobalt doesn't
			-- pick up mutations to the original table.
			return setmetatable(t, { __mode = mode })
		end

		-- Create a "new" string. This avoids interning via the constant table
		-- and the short string optimisation.
		local function mk_str(x) return x .. ("."):rep(100) end

		describe("with weak keys", function()
			it("will clear keys", function()
				local k = {}

				-- Set up our table
				local t = setmode({}, "k")
				t[k] = "value"

				expect(size(t)):eq(1)
				expect(t[k]):eq("value")

				-- Collect garbage with value on the stack - key should be present.
				collectgarbage()

				expect(size(t)):eq(1)
				expect(t[k]):eq("value")

				-- Collect garbage with value not on the stack - key should be absent.
				k = nil
				collectgarbage()

				expect(size(t)):eq(0)
			end)

			it("normal tables can become weak", function()
				-- Set up our table
				local t = {[{}] = "value"}
				expect(size(t)):eq(1)

				setmode(t, "k")
				collectgarbage()
				expect(size(t)):eq(0)
			end)

			it("weak tables can become strong", function()
				-- Create a weak table then GC to remove one of the keys.
				local t1 = {}
				local t = setmode({[t1] = "t1", [{}] = "t2"}, "k")
				collectgarbage()
				expect(size(t)):eq(1)

				-- Make the table strong.
				setmode(t, nil)

				-- Clear our table
				t1 = nil
				collectgarbage()
				expect(size(t)):eq(1)

				local k, v = next(t)
				expect(v):eq("t1")
			end)

			it("behaves like an ephemeron table :lua>=5.2 :!cobalt", function()
				local t = setmode({}, "k")

				local t1 = {}
				t[t1] = { t1 }
				t1 = nil

				collectgarbage()

				local k, v = next(t)
				expect(k):eq(nil)
			end)
		end)

		describe("with weak values", function()
			it("will clear their values", function()
				local s1 = mk_str "test string"
				local t1, t2 = {}, {}
				local t = setmode({}, "v")

				-- Set up our table
				t["string"] = s1
				t["table"] = t1
				t[1] = t2

				expect(#t):eq(1)
				expect(size(t)):eq(3)

				expect(t["string"]):eq(s1)
				expect(t["table"]):eq(t1)
				expect(t[1]):eq(t2)

				-- Collect garbage once with these values still on the stack - no change.
				collectgarbage()

				expect(t["string"]):eq(s1)
				expect(t["table"]):eq(t1)
				expect(t[1]):eq(t2)

				-- Collect garbage with these values on longer on the stack - the table should be cleared of GC values.
				s1, t1, t2 = nil, nil, nil

				--[[
				Note [Clearing stack values]
				~~~~~~~~~~~~~~~~~~~~~~~~~
				Some of these values may still be on the stack in an argument position. This in a Cobalt-specific bug:
				it does not occur in PUC Lua, as they only GC up to the stack top. We obviously cannot control that in
				Java, and clearing the stack would come with a small performance cost.
				]]
				do local nasty1, nasty2, nasty3 = nil, nil, nil end

				collectgarbage()

				expect(#t):eq(0)
				expect(size(t)):eq(1)

				expect(t["string"]):ne(nil)
				expect(t["table"]):eq(nil)
				expect(t[1]):eq(nil)
			end)

			it("can change mode", function()
				local t = { {}, "preserved" }

				-- Table contains our value
				expect(t[1]):ne(nil)

				-- Change mode and collect garbage - value should be removed.
				setmode(t, "v")
				collectgarbage()

				expect(t[1]):eq(nil)
				expect(t[2]):eq("preserved")
			end)
		end)

		describe("with weak keys and values", function()
			it("will clear values", function()
				local a1 = {}
				local k1, v1 = {}, {}
				local k2, v2 = {}, {}

				-- Set up our table
				local t = setmode({ a1, [k1] = v1, [k2] = v2 }, "kv")
				expect(size(t)):eq(3)
				expect(t[1]):eq(a1)
				expect(t[k1]):eq(v1)
				expect(t[k2]):eq(v2)

				-- Collect garbage once with entries still on the stack - no change.
				collectgarbage()

				expect(size(t)):eq(3)
				expect(t[1]):eq(a1)
				expect(t[k1]):eq(v1)
				expect(t[k2]):eq(v2)

				-- Collect garbage with these entries on longer on the stack - the table should be cleared.
				a1, k1, v2 = nil, nil, nil
				-- See Note [Clearing stack values]
				do local nasty1, nasty2, nasty3 = nil, nil, nil end
				collectgarbage()

				expect(size(t)):eq(0)
			end)

			it("can change mode", function()
				-- Set up our table
				local t = setmode({ key = {}, [{}] = "value", {}, preserved = true })
				collectgarbage()
				expect(size(t)):eq(4)

				-- Change to a weak table and ensure it is empty
				setmode(t, "kv")
				collectgarbage()

				expect(size(t)):eq(1)
				expect(t.preserved):eq(true)
			end)
		end)
	end)

	describe("rawlen :lua>=5.2", function()
		it("behaves identically to PUC Lua on sparse tables", function()
			expect(rawlen({[1]="e",[2]="a",[3]="b",[4]="c"})):eq(4)
			expect(rawlen({[1]="e",[2]="a",[3]="b",[4]="c",[8]="f"})):eq(8)
		end)
	end)

	describe("rawset", function()
		it("rejects a nil key", function()
			expect.error(rawset, {}, nil, 1):str_match("table index is nil")
		end)

		it("rejects a nan key", function()
			expect.error(rawset, {}, 0/0, 1):str_match("table index is NaN")
		end)
	end)

	describe("table.getn :lua==5.1", function()
		it("behaves identically to PUC Lua on sparse tables", function()
			expect(table.getn({[1]="e",[2]="a",[3]="b",[4]="c"})):eq(4)
			expect(table.getn({[1]="e",[2]="a",[3]="b",[4]="c",[8]="f"})):eq(8)
		end)
	end)

	describe("table.maxn :lua==5.1", function()
		it("behaves identically to PUC Lua on sparse tables", function()
			expect(table.maxn({[1]="e",[2]="a",[3]="b",[4]="c"})):eq(4)
			expect(table.maxn({[1]="e",[2]="a",[3]="b",[4]="c",[8]="f"})):eq(8)
		end)
	end)

	-- Test both directly on a table and via a proxy
	local function direct_and_proxy(name, func)
		it(name, function()
			local tbl = {}
			return func(function(x) return x end)
		end)

		it(name .. " (with metatable) :lua>=5.3", function()
			return func(function(tbl) return setmetatable({}, {
				__len = function() return #tbl end,
				__index = function(_, k) return tbl[k] end,
				__newindex = function(_, k, v) tbl[k] = v end,
			}) end)
		end)
	end

	describe("table.insert", function()
		direct_and_proxy("inserts at the beginning of the list", function(wrap)
			local function mk_expected(size)
				local out = {}
				for i = 1, size do out[i] = "Value #" .. (size - i + 1) end
				return out
			end

			local tbl = {}
			local proxy = wrap(tbl)
			for i = 1, 32 do
				table.insert(proxy, 1, "Value #" .. i)
				expect(tbl):same(mk_expected(i))
			end
		end)

		direct_and_proxy("inserts at the end of the list", function(wrap)
			local function mk_expected(size)
				local out = {}
				for i = 1, size do out[i] = "Value #" .. i end
				return out
			end

			local tbl = {}
			local proxy = wrap(tbl)
			for i = 1, 32 do
				table.insert(proxy, "Value #" .. i)
				expect(tbl):same(mk_expected(i))
			end
		end)

		direct_and_proxy("inserts in the middle of the list", function(wrap)
			local function mk_expected(size)
				local out = {}
				for i = 1, math.ceil(size / 2) do out[i] = "Value #" .. (i * 2 - 1) end
				for i = 1, math.floor(size / 2) do out[size - i + 1] = "Value #" .. (i * 2) end
				return out
			end

			local tbl = {}
			local proxy = wrap(tbl)
			for i = 1, 32 do
				table.insert(proxy, math.floor(i / 2) + 1, "Value #" .. i)
				expect(tbl):same(mk_expected(i))
			end
		end)
	end)

	describe("table.remove", function()
		it("removes values at 0 :lua>=5.2", function()
			local a = {[0] = "ban"}
			expect(#a):eq(0)
			expect(table.remove(a)):eq("ban")
			expect(a[0]):eq(nil)
		end)

		local function mk_filled()
			local out = {}
			for i = 1, 32 do out[i] = "Value #" .. i end
			return out
		end

		direct_and_proxy("remove at beginning of list", function(wrap)
			local function mk_expect(size)
				local out = {}
				for i = 1, size do out[i] = "Value #" .. (32 - size + i) end
				return out
			end

			local tbl = mk_filled(size)
			local proxy = wrap(tbl)

			for i = 1, 32 do
				expect(table.remove(proxy, 1)):eq("Value #" .. i)
				expect(tbl):same(mk_expect(32 - i))
			end
		end)

		direct_and_proxy("remove at end of list", function(wrap)
			local function mk_expect(size)
				local out = {}
				for i = 1, size do out[i] = "Value #" .. i end
				return out
			end

			local tbl = mk_filled(size)
			local proxy = wrap(tbl)

			for i = 1, 32 do
				expect(table.remove(proxy)):eq("Value #" .. (32 - i + 1))
				expect(tbl):same(mk_expect(32 - i))
			end
		end)
	end)

	describe("table.insert/table.remove PUC Lua tests", function()
		-- Combined tests of table.insert and table.remove from nextvar.

		-- Some assertions are commented out here, as we don't do the bounds checks that Lua 5.2 do.

		local function test(a)
			-- expect.error(table.insert, a, 2, 20)
			table.insert(a, 10); table.insert(a, 2, 20)
			table.insert(a, 1, -1); table.insert(a, 40)
			table.insert(a, #a+1, 50)
			table.insert(a, 2, -2)
			expect(a[2]):ne(nil)
			expect(a["2"]):eq(nil)
			-- expect.error(table.insert, a, 0, 20)
			-- expect.error(table.insert, a, #a + 2, 20)
			expect(table.remove(a,1)):eq(-1)
			expect(table.remove(a,1)):eq(-2)
			expect(table.remove(a,1)):eq(10)
			expect(table.remove(a,1)):eq(20)
			expect(table.remove(a,1)):eq(40)
			expect(table.remove(a,1)):eq(50)
			expect(table.remove(a,1)):eq(nil)
			expect(table.remove(a)):eq(nil)
			expect(table.remove(a, #a)):eq(nil)
		end

		it("test #1", function()
			local a = {n=0, [-7] = "ban"}
			test(a)
			expect(a.n):eq(0)
			expect(a[-7]):eq("ban")
		end)

		it("test #2", function()
			local a = {[-7] = "ban"};
			test(a)
			expect(a.n):eq(nil)
			expect(#a):eq(0)
			expect(a[-7] == "ban")
		end)

		it("test #3", function()
			local a = {[-1] = "ban"}
			test(a)
			expect(a.n):eq(nil)
			expect(table.remove(a)):eq(nil)
			expect(a[-1]):eq("ban")
		end)

		it("test #4", function()
			local a = {}
			table.insert(a, 1, 10); table.insert(a, 1, 20); table.insert(a, 1, -1)
			expect(table.remove(a)):eq(10)
			expect(table.remove(a)):eq(20)
			expect(table.remove(a)):eq(-1)
			expect(table.remove(a)):eq(nil)
		end)

		it("test #4", function()
			local a = {'c', 'd'}
			table.insert(a, 3, 'a')
			table.insert(a, 'b')
			expect(table.remove(a, 1)):eq('c')
			expect(table.remove(a, 1)):eq('d')
			expect(table.remove(a, 1)):eq('a')
			expect(table.remove(a, 1)):eq('b')
			expect(table.remove(a, 1)):eq(nil)
			expect(#a):eq(0) expect(a.n):eq(nil)
		end)

		it("test #5", function()
			local a = {10,20,30,40}
			expect(table.remove(a, #a + 1)):eq(nil)
			-- expect.error(table.remove, a, 0)
			expect(a[#a]):eq(40)
			expect(table.remove(a, #a)):eq(40)
			expect(a[#a]):eq(30)
			expect(table.remove(a, 2)):eq(20)
			expect(a[#a]):eq(30)
			expect(#a):eq(2)
		end)
	end)

	describe("table.move :lua>=5.3", function()
		direct_and_proxy("moves forward", function(wrap)
			local tbl = { 10, 20, 30 }
			table.move(wrap(tbl), 1, 3, 2)
			expect(tbl):same { 10, 10, 20, 30 }
		end)

		direct_and_proxy("moves forward with overlap", function(wrap)
			local tbl = { 10, 20, 30 }
			table.move(wrap(tbl), 1, 3, 3)
			expect(tbl):same { 10, 20, 10, 20, 30 }
		end)

		direct_and_proxy("moves forward to new table", function(wrap)
			local tbl = { 10, 20, 30 }
			local new = {}
			table.move(wrap(tbl), 1, 10, 1, wrap(new))
			expect(new):same { 10, 20, 30 }
		end)

		-- We do test this above too, but this is a more explicit test.
		it("uses metamethods", function()
			local a = setmetatable({}, {
				__index = function (_,k) return k * 10 end,
				__newindex = error
			})
			local b = table.move(a, 1, 10, 3, {})
			expect(a):same {}
			expect(b):same { nil,nil,10,20,30,40,50,60,70,80,90,100 }

		  	local b = setmetatable({""}, {
				__index = error,
				__newindex = function (t,k,v) t[1] = string.format("%s(%d,%d)", t[1], k, v) end
			})

			table.move(a, 10, 13, 3, b)
			expect(b[1]):eq "(3,100)(4,110)(5,120)(6,130)"
			expect.error(table.move, b, 10, 13, 3, b):eq(b)
		end)

		it("copes close to overflow", function()
			local a = table.move({[maxI - 2] = 1, [maxI - 1] = 2, [maxI] = 3}, maxI - 2, maxI, -10, {})
			expect(a):same {[-10] = 1, [-9] = 2, [-8] = 3}

			local a = table.move({[minI] = 1, [minI + 1] = 2, [minI + 2] = 3}, minI, minI + 2, -10, {})
			expect(a):same { [-10] = 1, [-9] = 2, [-8] = 3 }

			local a = table.move({45}, 1, 1, maxI)
			expect(a):same { 45, [maxI] = 45 }

			local a = table.move({[maxI] = 100}, maxI, maxI, minI)
			expect(a):same { [minI] = 100, [maxI] = 100 }

			local a = table.move({[minI] = 100}, minI, minI, maxI)
			expect(a):same { [minI] = 100, [maxI] = 100 }
		end)

		it("copes with large numbers", function()
			local function checkmove (f, e, t, x, y)
				local pos1, pos2
				local a = setmetatable({}, {
					__index = function (_,k) pos1 = k end,
					__newindex = function (_,k) pos2 = k; error() end
				})
				local st, msg = pcall(table.move, a, f, e, t)
				expect(st):eq(false)
				expect(msg):eq(nil)
				expect(pos1):eq(x)
				expect(pos2):eq(y)
			end

			checkmove(1, maxI, 0, 1, 0)
			checkmove(0, maxI - 1, 1, maxI - 1, maxI)
			checkmove(minI, -2, -5, -2, maxI - 6)
			checkmove(minI + 1, -1, -2, -1, maxI - 3)
			checkmove(minI, -2, 0, minI, 0)  -- non overlapping
			checkmove(minI + 1, -1, 1, minI + 1, 1)  -- non overlapping
		end)

		it("errors on overflow :lua~=5.4", function()
			expect.error(table.move, {}, 0, maxI, 1):str_match("too many")
			expect.error(table.move, {}, -1, maxI - 1, 1):str_match("too many")
			expect.error(table.move, {}, minI, -1, 1):str_match("too many")
			expect.error(table.move, {}, minI, maxI, 1):str_match("too many")
			expect.error(table.move, {}, 1, maxI, 2):str_match("wrap around")
			expect.error(table.move, {}, 1, 2, maxI):str_match("wrap around")
			expect.error(table.move, {}, minI, -2, 2):str_match("wrap around")
		end)
	end)

	describe("table.sort", function()
		local function check(a, f)
			f = f or function(x, y) return x < y end
			for i = #a, 2, -1 do
				local x, y = a[i], a[i - 1]
				if f(x, y) then
					fail(("%s, %s at %d are out of order"):format(x, y, i))
				end
			end
		end

		it("a basic sort", function()
			local a = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"}
			table.sort(a)
			check(a)
		end)

		it("various permutations", function()
			local unpack = table.unpack or unpack
			local function perm (s, n)
				n = n or #s
				if n == 1 then
					local t = {unpack(s)}
					table.sort(t)
					check(t)
				else
					for i = 1, n do
					s[i], s[n] = s[n], s[i]
					perm(s, n - 1)
					s[i], s[n] = s[n], s[i]
					end
				end
			end

			perm {}
			perm {1}
			perm {1,2}
			perm {1,2,3}
			perm {1,2,3,4}
			perm {2,2,3,4}
			perm {1,2,3,4,5}
			perm {1,2,3,3,5}
			perm {1,2,3,4,5,6}
			perm {2,2,3,3,5,6}
		end)

		it("a long list of items", function()
			local limit = 30000
			local a = {}
			for i = 1, limit do a[i] = math.random() end
			table.sort(a)
			check(a)
		end)

		it("reverse sort", function()
			local limit = 30000
			local a = {}
			for i = 1, limit do a[i] = math.random() end

			table.sort(a, function(x, y) return y < x end)
			check(a, function(x, y) return y < x end)
		end)

		it("equal items", function()
			local limit = 30000
			local a = {}
			for i = 1, limit do a[i] = false end

			table.sort(a, function(x,y) return nil end)
		end)

		it("invalid sort order :lua>=5.2 :!cobalt", function()
			local function check (t)
				local function f(a, b) assert(a and b); return true end
				expect.error(table.sort, t, f):eq("invalid order function for sorting")
			end

			check {1,2,3,4}
			check {1,2,3,4,5}
			check {1,2,3,4,5,6}
		end)

		it("behaves identically to PUC Lua on sparse tables", function()
			local test = {[1]="e",[2]="a",[3]="d",[4]="c",[8]="b"}

			table.sort(test, function(a, b)
				if not a then
					return false
				end
				if not b then
					return true
				end
				return a < b
			end)

			expect(test):same { "a", "b", "c", "d", "e" }
		end)

		it("uses metatables :lua>=5.3", function()
			local original = { "e", "d", "c", "b", "a" }
			local slice = make_slice(original, 2, 3)

			table.sort(slice)
			expect(original):same { "e", "b", "c", "d", "a" }
			expect(next(slice)):eq(nil)
		end)

		it("ignores negative lengths :lua>=5.2", function()
			local t = setmetatable({}, { __len = function() return -1 end })
			expect(#t):eq(-1)
			table.sort(t, function() fail("Unexpected comparison") end)
		end)

		it("supports yielding in the comparator :cobalt", function()
			expect.run_coroutine(function()
				local x = { 32, 2, 4, 13 }
				table.sort(x, function(a, b)
					local x, y = coroutine.yield(a, b)
					expect(x):eq(a)
					expect(y):eq(b)

					return a < b
				end)

				expect(x[1]):eq(2)
				expect(x[2]):eq(4)
				expect(x[3]):eq(13)
				expect(x[4]):eq(32)
			end)
		end)

		it("supports yielding in the metamethod :cobalt", function()
			local meta = {
				__lt = function(a, b)
					local x, y = coroutine.yield(a, b)
					expect(x):eq(a)
					expect(y):eq(b)

					return a.x < b.x
				end,
			}

			local function create(val) return setmetatable({ x = val }, meta) end

			expect.run_coroutine(function()
				local x = { create(32), create(2), create(4), create(13) }
				table.sort(x)

				expect(x[1].x):eq(2)
				expect(x[2].x):eq(4)
				expect(x[3].x):eq(13)
				expect(x[4].x):eq(32)
			end)
		end)
	end)

	describe("table.pack", function()
		it("counts nils :lua>=5.2", function()
			expect(table.pack(1, "foo", nil, nil)):same { n = 4, 1, "foo" }
		end)
	end)

	describe("table.unpack", function()
		it("accepts nil arguments :lua>=5.2", function()
			local a, b, c = table.unpack({ 1, 2, 3, 4, 5 }, nil, 2)
			expect(a):eq(1)
			expect(b):eq(2)
			expect(c):eq(nil)

			local a, b, c = table.unpack({ 1, 2 }, nil, nil)
			expect(a):eq(1)
			expect(b):eq(2)
			expect(c):eq(nil)
		end)

		it("some basic functionality :lua>=5.2", function()
			-- Largely copied from sort.lua
			local a, lim = {}, 2000
			for i = 1, lim do a[i] = i end

			expect(select(lim, table.unpack(a))):eq(lim)
			expect(select('#', table.unpack(a))):eq(lim)

			local x, y, z

			x = table.unpack(a)
			expect(x):eq(1)

			x = {table.unpack(a)}
			expect(#x):eq(lim) expect(x[1]):eq(1) expect(x[lim]):eq(lim)

			x = {table.unpack(a, lim-2)}
			expect(#x):eq(3) expect(x[1]):eq(lim-2) expect(x[3]):eq(lim)

			x = {table.unpack(a, 10, 6)}
			expect(next(x)):eq(nil)   -- no elements

			x = {table.unpack(a, 11, 10)}
			expect(next(x)):eq(nil)   -- no elements

			x, y = table.unpack(a, 10, 10)
			expect(x):eq(10) expect(y):eq(nil)

			x, y, z = table.unpack(a, 10, 11)
			expect(x):eq(10) expect(y):eq(11) expect(z):eq(nil)

			a, x = table.unpack{1}
			expect(a):eq(1) expect(x):eq(nil)

			a, x = table.unpack({1,2}, 1, 1)
			expect(a):eq(1) expect(x):eq(nil)
		end)

		it("on large values :lua>=5.2", function()
			local maxi = (2 ^ 31) - 1 -- maximum value for an int
  			local mini = -(2 ^ 31)    -- minimum value for an int
			expect.error(table.unpack, {}, 0, maxi):eq("too many results to unpack")
			expect.error(table.unpack, {}, 1, maxi):eq("too many results to unpack")
			expect.error(table.unpack, {}, 0, maxI):eq("too many results to unpack")
			expect.error(table.unpack, {}, 1, maxI):eq("too many results to unpack")
			expect.error(table.unpack, {}, mini, maxi):eq("too many results to unpack")
			expect.error(table.unpack, {}, -maxi, maxi):eq("too many results to unpack")
			expect.error(table.unpack, {}, minI, maxI):eq("too many results to unpack")
			table.unpack({}, maxi, 0)
			table.unpack({}, maxi, 1)
			table.unpack({}, maxI, minI)
			pcall(table.unpack, {}, 1, maxi + 1)
			local a, b = table.unpack({[maxi] = 20}, maxi, maxi)
			expect(a):eq(20) expect(b):eq(nil)
			a, b = table.unpack({[maxi] = 20}, maxi - 1, maxi)
			expect(a):eq(nil) expect(b):eq(20)
			local t = {[maxI - 1] = 12, [maxI] = 23}
			a, b = table.unpack(t, maxI - 1, maxI); assert(a == 12 and b == 23)
			a, b = table.unpack(t, maxI, maxI); assert(a == 23 and b == nil)
			a, b = table.unpack(t, maxI, maxI - 1); assert(a == nil and b == nil)
			t = {[minI] = 12.3, [minI + 1] = 23.5}
			a, b = table.unpack(t, minI, minI + 1); assert(a == 12.3 and b == 23.5)
			a, b = table.unpack(t, minI, minI); assert(a == 12.3 and b == nil)
			a, b = table.unpack(t, minI + 1, minI); assert(a == nil and b == nil)
		end)

		it("takes slices of tables :lua>=5.2", function()
			expect(table.pack(table.unpack({ 1, "foo" }))):same { n = 2, 1, "foo" }
			expect(table.pack(table.unpack({ 1, "foo" }, 2))):same { n = 1, "foo" }
			expect(table.pack(table.unpack({ 1, "foo" }, 2, 5))):same { n = 4, "foo" }
		end)

		it("uses metamethods :lua>=5.3", function()
			local basic = make_slice({ "a", "b", "c", "d", "e" }, 2, 3)
			expect(table.pack(table.unpack(basic))):same { n = 3, "b", "c", "d" }
			expect(table.pack(table.unpack(basic, 2))):same { n = 2, "c", "d" }
		end)
	end)

	describe("table.concat", function()
		it("returns empty strings on empty tables", function()
			expect(table.concat{}):eq("")
			expect(table.concat({}, 'x')):eq("")
		end)

		it("works with \\0", function()
			expect(table.concat({'\0', '\0\1', '\0\1\2'}, '.\0.')):eq("\0.\0.\0\1.\0.\0\1\2")
		end)

		it("accepts various ranges", function()
			local a = {}; for i=1,3000 do a[i] = "xuxu" end
			expect(table.concat(a, "123").."123"):eq(string.rep("xuxu123", 3000))
			expect(table.concat(a, "b", 20, 20)):eq("xuxu")
			expect(table.concat(a, "", 20, 21)):eq("xuxuxuxu")
			expect(table.concat(a, "", 22, 21)):eq("")
			expect(table.concat(a, "x", 22, 21)):eq("")
			expect(table.concat(a, "3", 2999)):eq("xuxu3xuxu")

			local a = {"a","b","c"}
			expect(table.concat(a, ",", 1, 0)):eq("")
			expect(table.concat(a, ",", 1, 1)):eq("a")
			expect(table.concat(a, ",", 1, 2)):eq("a,b")
			expect(table.concat(a, ",", 2)):eq("b,c")
			expect(table.concat(a, ",", 3)):eq("c")
			expect(table.concat(a, ",", 4)):eq("")
		end)

		it("avoids integer overflow :!cobalt", function()
			expect(table.concat({}, "x", 2^31-1, 2^31-2)):eq("")
			expect(table.concat({}, "x", -2^31+1, -2^31)):eq("")
			expect(table.concat({}, "x", 2^31-1, -2^31)):eq("")
			expect(table.concat({[2^31-1] = "alo"}, "x", 2^31-1, 2^31-1)):eq("alo")
		end)

		it("errors on non-strings :!cobalt", function()
			expect.error(table.concat, {"a", "b", {}})
				:eq("invalid value (table) at index 3 in table for 'concat'")
		end)

		it("errors on non-strings :cobalt", function()
			-- FIXME: This is entirely wrong!
			expect.error(table.concat, {"a", "b", {}})
				:eq("bad argument (string expected, got table)")
		end)

		it("uses metamethods :lua>=5.3", function()
			local basic = make_slice({ "a", "b", "c", "d", "e" }, 2, 3)
			expect(table.concat(basic)):eq("bcd")
		end)
	end)

	describe("table.foreach :lua==5.1", function()
		it("supports yielding :cobalt", function()
			expect.run_coroutine(function()
				local x = { 3, "foo", 4, 1 }
				local idx = 1
				table.foreach(x, function(key, val)
					expect(key):eq(idx)
					expect(val):eq(x[idx])
					expect(coroutine.yield(val)):eq(val)

					idx = idx + 1
				end)
			end)
		end)
	end)

	describe("table.foreachi :lua==5.1", function()
		it("supports yielding :cobalt", function()
			expect.run_coroutine(function()
				local x = { 3, "foo", 4, 1 }
				local idx = 1
				table.foreachi(x, function(key, val)
					expect(key):eq(idx)
					expect(val):eq(x[idx])
					expect(coroutine.yield(val)):eq(val)

					idx = idx + 1
				end)
			end)
		end)
	end)
end)
