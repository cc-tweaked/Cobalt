--- Ensure the resulting name of debug.getinfo is correct

local function expect_eq(actual, exp)
	if actual ~= exp then
		error(("Error:\nExpected %s,\n     got %s"):format(exp, actual), 2)
	end
end

-- When tail called
local function get_name()
	return debug.getinfo(1).name
end

local function no_tail()
	return (get_name())
end
local function tail()
	return get_name()
end

expect_eq(no_tail(), "get_name")
expect_eq(tail(), nil)

-- And within hooks
debug.sethook(function()
	local info = debug.getinfo(1)
	expect_eq(info.name, "?")
	expect_eq(info.namewhat, "hook")
end, "l");
(function() end)();
debug.sethook(nil, "lcr", 1)
