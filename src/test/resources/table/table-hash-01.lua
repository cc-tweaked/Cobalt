--- Tests a regression with table hashing

local queue, hash = {}, {}
for i = 1, 5e6 do
	if math.random() > 0.3 then
		local id = math.random(1, 2147483647)
		hash[id] = true
		queue[#queue + 1] = id
	end

	if #queue > 1 and math.random() > 0.2 then
		local id = table.remove(queue, 1)
		hash[id] = nil
	end
end
