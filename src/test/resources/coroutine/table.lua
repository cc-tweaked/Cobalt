-- Test yielding within foreach
run(function()
	local x = { 3, "foo", 4, 1 }
	local idx = 1
	table.foreach(x, function(key, val)
		assertEquals(idx, key)
		assertEquals(x[idx], val)
		assertEquals(val, coroutine.yield(val))

		idx = idx + 1
	end)
end)

-- Test yielding within foreachi
run(function()
	local x = { 3, "foo", 4, 1 }
	local idx = 1
	table.foreachi(x, function(key, val)
		assertEquals(idx, key)
		assertEquals(x[idx], val)
		assertEquals(val, coroutine.yield(val))

		idx = idx + 1
	end)
end)

-- Test yielding inside table.sort comparator
run(function()
	local x = { 32, 2, 4, 13 }
	table.sort(x, function(a, b)
		local x, y = coroutine.yield(a, b)
		assertEquals(a, x)
		assertEquals(b, y)

		return a < b
	end)

	assertEquals(2, x[1])
	assertEquals(4, x[2])
	assertEquals(13, x[3])
	assertEquals(32, x[4])
end)

-- Test yielding within metatable comparator
local meta = {
	__lt = function(a, b)
		local x, y = coroutine.yield(a, b)
		assertEquals(a, x)
		assertEquals(b, y)

		return a.x < b.x
	end
}

local function create(val) return setmetatable({ x = val }, meta) end

run(function()
	local x = { create(32), create(2), create(4), create(13) }
	table.sort(x)

	assertEquals(2, x[1].x)
	assertEquals(4, x[2].x)
	assertEquals(13, x[3].x)
	assertEquals(32, x[4].x)
end)

run(function()
	local original = { "e", "d", "c", "b", "a" }
	local slice = setmetatable({}, {
		__len = function(self) return coroutine.yield(3) end,
		__index = function(self, n) return original[coroutine.yield(n) + 1] end,
		__newindex = function(self, n, x) original[coroutine.yield(n) + 1] = x end,
	})

	table.sort(slice)

	assert(table.concat(original) == "ebcda")
	assert(next(slice) == nil)
end)
