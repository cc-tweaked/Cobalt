-- Ensure that yielding within a xpcall works as expected
run(function()
	local ok, a, b, c = xpcall(function()
		return coroutine.yield(1, 2, 3)
	end, function(msg) return msg .. "!" end)

	assertEquals(true, ok)
	assertEquals(1, a)
	assertEquals(2, b)
	assertEquals(3, c)
end)

-- Ensure that yielding then erroring inside xpcall works as expected
run(function()
	local ok, msg = xpcall(function()
		local a, b, c = coroutine.yield(1, 2, 3)
		assertEquals(1, a)
		assertEquals(2, b)
		assertEquals(3, c)

		error("Error message")
	end, function(msg) return msg .. "!" end)

	assertEquals(false, ok)
	assertEquals("xpcall.lua:21: Error message!", msg)
end)

-- Ensure that erroring inside the error handler
run(function()
	local ok, msg = xpcall(function()
		local a, b, c = coroutine.yield(1, 2, 3)
		assertEquals(1, a)
		assertEquals(2, b)
		assertEquals(3, c)

		error("Error message")
	end, function(msg) error(msg) end)

	assertEquals(false, ok)
	assertEquals("error in error handling", msg)
end)

-- Ensure that yielding inside the error handler works as expected
run(function()
	local ok, msg = xpcall(function()
		local a, b, c = coroutine.yield(1, 2, 3)
		assertEquals(1, a)
		assertEquals(2, b)
		assertEquals(3, c)

		error("Error message")
	end, function(msg)
		return coroutine.yield(msg) .. "!"
	end)

	assertEquals(false, ok)
	assertEquals("xpcall.lua:51: Error message!", msg)
end)

-- Ensure that yielding then erroring inside the error handling works as expected
run(function()
	local ok, msg = xpcall(function()
		local a, b, c = coroutine.yield(1, 2, 3)
		assertEquals(1, a)
		assertEquals(2, b)
		assertEquals(3, c)

		error("Error message")
	end, function(msg)
		assertEquals("xpcall.lua:68: Error message", coroutine.yield(msg))
		error("nope")
	end)

	assertEquals(false, ok)
	assertEquals("error in error handling", msg)
end)
