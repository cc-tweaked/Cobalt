for i = 1, 5 do
	local res = coroutine.yield()
	assert(res == "Resume " .. i)
end
