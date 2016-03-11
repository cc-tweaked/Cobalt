--- Horrible strings in Lua
local function check(func)
	local success, message = pcall(function()
		func(("a"):rep(1e4), ".-.-.-.-b$")
	end)

	assert(not success, "Expected abort")
	assert(message:find("Timed out"), "Got " .. message)
end

check(string.find)
check(string.match)
check(function(...) string.gmatch(...)() end)
check(function(a, b) string.gsub(a, b, "") end)
