-- Basic test to ensure that yielding works as expected
run(function()
	assertEquals(1, coroutine.yield(1))

	local x, y = coroutine.yield(1, 2)
	assertEquals(1, x)
	assertEquals(2, y)
end)
