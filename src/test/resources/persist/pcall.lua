local function go(a, b)
	suspend()
	return a + b
end

local ok, res = pcall(go, 1, 2)
assert(ok)
assert(res == 3)

return "OK"
