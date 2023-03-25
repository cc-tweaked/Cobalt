describe("Lua tables", function()
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

	describe("table.sort", function()
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
	end)

	describe("table.pack", function()
		it("counts nils :lua>=5.2", function()
			expect(table.pack(1, "foo", nil, nil)):same { n = 4, 1, "foo" }
		end)
	end)

	describe("table.unpack", function()
		it("accepts nil arguments :lua>=5.2", function()
			local a, b, c = table.unpack({ 1, 2, 3, 4, 5 }, nil, 2)
			assert(a == 1)
			assert(b == 2)
			assert(c == nil)

			local a, b, c = table.unpack({ 1, 2 }, nil, nil)
			assert(a == 1)
			assert(b == 2)
			assert(c == nil)
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
		it("uses metamethods :lua>=5.3", function()
			local basic = make_slice({ "a", "b", "c", "d", "e" }, 2, 3)
			expect(table.concat(basic)):eq("bcd")
		end)
	end)
end)
