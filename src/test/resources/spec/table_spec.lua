describe("Lua tables", function()
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
