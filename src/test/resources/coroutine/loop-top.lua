for i = 1, 5 do
	local res = yieldBlocking()
	assert(res == "Resume " .. i)
end
