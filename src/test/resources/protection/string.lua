-- Test string matching
local function check(...)
	local success, message = pcall(...)

	assert(not success, "Expected abort")
	assert(message:find("Timed out"), "Got " .. message)
end

local function checkString(func)
	check(func, ("a"):rep(1e4), ".-.-.-.-b$")
end

checkString(string.find)
checkString(string.match)
checkString(function(...) string.gmatch(...)() end)
checkString(function(a, b) string.gsub(a, b, "") end)

