--- Test loading long strings

local function check(...)
	local success, message = pcall(...)

	assert(not success, "Expected abort")
	assert(message:find("Timed out"), "Got " .. message)
end

check(function()
	local fn, err = load("--[" .. ("="):rep(1e8) .. "[")
	print(fn, err)
end)
