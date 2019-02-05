--- Test yielding within a tail call
run(function()
	local function yieldTail(n)
		assertEquals(1, coroutine.yield(1))
		assertEquals(2, coroutine.yield(2))

		if n > 0 then
			return yieldTail(n - 1)
		end
	end

	yieldTail(5)
end)
