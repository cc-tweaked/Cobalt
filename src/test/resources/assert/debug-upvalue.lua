local a = 0
local function f() return a end

assert(not pcall(debug.upvalueid, f, 0))
assert(not pcall(debug.upvalueid, f, 2))
assert(type(debug.upvalueid(f, 1)) == "userdata")

local f, g = (function()
	local a = 0
	local function f() return a end
	local function g() return a end

	assert(debug.upvalueid(f, 1) == debug.upvalueid(g, 1), "Equal within their closure")
	return f, g, a
end)()
assert(debug.upvalueid(f, 1) == debug.upvalueid(g, 1), "Equal after begin closed")

do
	local function make_incr()
		local a = 0
		return function() a = a + 1; return a end
	end

	local a, b = make_incr(), make_incr()

	assert(a() == 1)
	assert(a() == 2)
	assert(b() == 1)

	assert(debug.upvalueid(a, 1) ~= debug.upvalueid(b, 1))
	debug.upvaluejoin(a, 1, b, 1)
	assert(debug.upvalueid(a, 1) == debug.upvalueid(b, 1))

	assert(a() == 2)
	assert(b() == 3)
end
