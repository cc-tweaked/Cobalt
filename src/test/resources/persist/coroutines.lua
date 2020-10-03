local function worker()
	local a = coroutine.yield()
	suspend()
	local b = coroutine.yield()

	return a + b
end

local h = coroutine.create(worker)
assert(coroutine.resume(h))
assert(coroutine.resume(h, 1))

local ok, res = coroutine.resume(h, 2)
assert(ok, res)
assert(res == 3)
assert(coroutine.status(h) == "dead")

return "OK"
