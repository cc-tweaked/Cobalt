--- Tests that tail calls are validated at the call site, rather than
-- after returning.

local function first()
	local first_var = 1
	return first_var()
end

local function second()
	local _
	first()
end

local ok, err = pcall(second)
assert(not ok, "Should have failed")
assert(err == "invalid-tailcall.lua:6: attempt to call local 'first_var' (a number value)", err)
