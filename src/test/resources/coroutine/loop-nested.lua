local co1 = coroutine.create(function()
	assert(coroutine.yield("Co 1 [Yield]") == "Co 1 [Resume]")
	return "Co 1 [Done]"
end)

local co2 = coroutine.create(function()
	assert(coroutine.yield("Co 2 [Yield]") == "Co 2 [Resume]")
	return "Co 2 [Done]"
end)

local ok, res

ok, res = coroutine.resume(co1)
assert(ok and res == "Co 1 [Yield]")

ok, res = coroutine.resume(co2)
assert(ok and res == "Co 2 [Yield]")

res = coroutine.yield()
assert(res == "Resume 1")

ok, res = coroutine.resume(co1, "Co 1 [Resume]")
assert(ok and res == "Co 1 [Done]")

ok, res = coroutine.resume(co2, "Co 2 [Resume]")
assert(ok and res == "Co 2 [Done]")
