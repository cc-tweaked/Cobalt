--- Tests that debug hooks are not propagated to child coroutines
-- While the hook themselves are propagated, the registry HOOKKEY isn't. Consequently
-- only native hooks are propagated in practice.
local function testHook(a) end
debug.sethook(testHook, "c")

local c = coroutine.create(function()
	return debug.gethook()
end)

local ok, hook = coroutine.resume(c)

debug.sethook()

assert(debug.gethook() == nil)
assert(ok and hook == nil)
