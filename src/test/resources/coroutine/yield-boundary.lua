--- Test we can yield across noUnwind boundaries
run(function()
	noUnwind(function()
		assertEquals("test", coroutine.yield("test"))
	end)
end)
