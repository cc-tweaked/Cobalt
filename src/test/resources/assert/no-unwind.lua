-- load effectively acts as a pcall, but didn't pop the stack as expected
(function()
	local ok, err = pcall(function()
		load(function()
			error("Oh dear")
		end)
	end)
	assert(ok, err)
end)()

