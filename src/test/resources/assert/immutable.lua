local function id(...)
	return ...
end

local function run(f)
	local co = coroutine.create(f)
	local function resume(ok, ...)
		if not ok then error(debug.traceback(co, ...), 0) end
		if coroutine.status(co) == "dead" then return end
		return resume(coroutine.resume(co, ...))
	end

	return resume(true)
end

run(function()
	local a, b, c

	a, b, c = id(1, 2, 3)
	assert(a == 1 and b == 2 and c == 3, ("%d, %d, %d"):format(a, b, c))

	a, b, c = id_(1, 2, 3)
	assert(a == 1 and b == 2 and c == 3, ("%d, %d, %d"):format(a, b, c))

	a, b, c = coroutine.yield(1, 2, 3)
	assert(a == 1 and b == 2 and c == 3, ("%d, %d, %d"):format(a, b, c))
end)
