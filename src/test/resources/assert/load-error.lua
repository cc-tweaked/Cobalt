-- Ensures load returns the original error value

local tbl = {}
local fun, err = load(function()
	error(tbl)
end)

assert(fun == nil)
assert(err == tbl)
