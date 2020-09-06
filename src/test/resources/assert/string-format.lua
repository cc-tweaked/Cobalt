local res = setmetatable({}, { __tostring = function()
	return "Test __tostring"
end })
assert(("%s"):format(res) == "Test __tostring")

local c = coroutine.create(function()
	local res = setmetatable({}, {
		__tostring = function()
			coroutine.yield()
			return "Test __tostring yield"
		end
	})
	assert(("%s"):format(res) == "Test __tostring yield")
end)

while coroutine.status(c) ~= "dead" do
	assert(coroutine.resume(c))
end
