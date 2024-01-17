-- Ensure that yielding within a debug hook works as expected
run(function()
	local counts, depth = {}, 0
	debug.sethook(function(kind)
		counts[kind] = (counts[kind] or 0) + 1

		if kind == "return" then depth = depth - 1 end
		print(("%s%-10s %s"):format(("  "):rep(depth), kind .. " (" .. counts[kind] .. ")", debug.getinfo(2).source))
		if kind == "call" then depth = depth + 1 end

		assertEquals(kind, coroutine.yield(kind))
	end, "crl", 1)

	assertEquals("zyz", (string.gsub("xyz", "x", "z")))
	assertEquals(true, pcall(function()
		local x = 0
		for i = 1, 5 do x = x + i end
	end))

	debug.sethook(nil)
	assertEquals(6, counts.call)
	assertEquals(6, counts['return'])
	assertEquals(36, counts.count)
	assertEquals(36, counts.line)
end)
