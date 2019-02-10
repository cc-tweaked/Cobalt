-- Ensure that yielding within a pcall works as expected
run(function()
	local ok, a, b, c = pcall(function()
		return coroutine.yield(1, 2, 3)
	end)

	assertEquals(true, ok)
	assertEquals(1, a)
	assertEquals(2, b)
	assertEquals(3, c)
end)

-- Ensure that yielding then erroring inside pcall works as expected
run(function()
	local ok, msg = pcall(function()
		local a, b, c = coroutine.yield(1, 2, 3)
		assertEquals(1, a)
		assertEquals(2, b)
		assertEquals(3, c)

		error("Error message", 0)
	end)

	assertEquals(false, ok)
	assertEquals("Error message", msg)
end)
