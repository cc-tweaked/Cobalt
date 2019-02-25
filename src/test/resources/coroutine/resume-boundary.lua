--- Test we can resume across noUnwind boundaries
noUnwind(function()
	local check = coroutine.create(function(x)
		assertEquals(1, x)
		assertEquals(2, coroutine.yield())
		assertEquals(3, coroutine.yield())
	end)

	local x = 0
	for i in coroutine.wrap(function()
		coroutine.yield(1)
		coroutine.yield(2)
		coroutine.yield(3)
	end) do
		x = x + 1
		assertEquals(x, i)
		assert(coroutine.resume(check, x))
	end

	assertEquals(x, 3)
end)

