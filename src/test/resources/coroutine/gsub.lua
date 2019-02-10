-- Ensure that yielding within a gsub works as expected
run(function()
	local result, count = ("hello world"):gsub("%w", function(entry)
		local x = coroutine.yield(entry)
		return x:upper()
	end)

	assertEquals("HELLO WORLD", result)
	assertEquals(10, count)
end)
