local function same(left, right)
	for k, v in pairs(left) do
		if v ~= right[k] then
			error(("Mismatch in key %s (%s vs %s)"):format(tostring(k), tostring(left[k]), tostring(right[k])), 2)
		end
	end

	for k, v in pairs(right) do
		if v ~= right[k] then
			error(("Mismatch in key %s (%s vs %s)"):format(tostring(k), tostring(left[k]), tostring(right[k])), 2)
		end
	end
end

same(table.pack(1, "foo", nil, nil), { n = 4, 1, "foo" })

-- table.unpack works in the basic case
same(table.pack(table.unpack({ 1, "foo" })), { n = 2, 1, "foo" })
same(table.pack(table.unpack({ 1, "foo" }, 2)), { n = 1, "foo" })
same(table.pack(table.unpack({ 1, "foo" }, 2, 5)), { n = 4, "foo" })

-- table.unpack invokes metamethods. It'd be cleaner to do this on tables,
-- but we don't support __len yet.
for i = 0, 255 do
	string[i] = i + 1
end
same(table.pack(table.unpack("aaa")), { n = 3, 2, 3, 4 })
same(table.pack(table.unpack("aaa", 2)), { n = 2, 3, 4 })

-- As above, but with a custom __index method.
debug.setmetatable(false, {
	__len = function()
		coroutine.yield()
		return 3
	end,
	__index = function(_, i)
		coroutine.yield()
		return i + 1
	end,
})

local go = coroutine.create(function()
	same(table.pack(table.unpack(false)), { n = 3, 2, 3, 4 })
end)

local count = 0
while coroutine.status(go) ~= "dead" do
	local ok, err = coroutine.resume(go)
	if not ok then
		error(err, 0)
	end
	count = count + 1
end

assert(count == 5) -- initial call + 4 yields
