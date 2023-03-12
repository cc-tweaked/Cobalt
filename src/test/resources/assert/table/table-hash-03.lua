--- Tests a regression with table hashing
-- TODO: Get this in a state where it's actually useful, it's too slow to be useful right now.

local hash = setmetatable({}, { __mode = "k" })
local size = 0
local count = 3e4
for i = 1, count do
	if (i % 1e3) == 0 then print(("%5.1f%%"):format(i / count * 100), size) end
	local r = math.random()

	if r > 0.3 then
		local id = {}
		size = size + 1
		hash[id] = true
	end

	if r > 0.2 then
		local k = next(hash)
		while k and math.random() > 0.5 do
			k = next(hash, k)
		end

		if k then
			hash[k] = nil
			size = size - 1
		end
	end

	if r > 0.9 then
		collectgarbage()
	end
end
