--- Test we can yield across noUnwind boundaries
run(function()
	local s, i = "RETURN 'hello'", 0
	local fn, err = load(function()
		i = i + 1
		local x = coroutine.yield(s:sub(i, i))
		return x and x:lower()
	end)
	assert(fn, err)
	assertEquals(fn(), "hello")
end)
