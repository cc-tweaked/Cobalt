-- Test infinite loops

local function check(...)
	local success, message = pcall(...)

	assert(not success, "Expected abort")
	assert(message:find("Timed out"), "Got " .. message)
end

check(function()
	while true do end
end)
