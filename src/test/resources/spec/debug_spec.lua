describe("The debug library", function()
	describe("Debug hooks", function()
		it("are not propagated to other coroutines", function()
			-- Tests that debug hooks are not propagated to child coroutines
			-- While the hook themselves are propagated, the registry HOOKKEY
			-- isn't. Consequently only native hooks are propagated in practice.
			local function hook(a) end
			debug.sethook(hook, "c")

			local c = coroutine.create(function() return debug.gethook() end)

			local ok, hook = coroutine.resume(c)

			debug.sethook()

			expect(debug.gethook()):eq(nil)
			expect(ok):eq(true)
			expect(hook):eq(nil)
		end)

		it("can yield within hooks :cobalt", function()
			expect.run_coroutine(function()
				local counts = { call = 0, ['return'] = 0, count = 0, line = 0 }

				debug.sethook(function(kind)
					counts[kind] = (counts[kind] or 0) + 1
					expect(coroutine.yield(kind)):eq(kind)
				end, "crl", 1)

				expect(string.gsub("xyz", "x", "z")):eq("zyz")
				expect(pcall(function()
					local x = 0
					for i = 1, 5 do x = x + i end
				end)):eq(true)

				debug.sethook(nil)

				-- These numbers are going to vary beyond the different VMs a
				-- little. As long as they're non-0, it's all fine.
				expect(counts.call):ne(0)
				expect(counts['return']):ne(0)
				expect(counts.count):ne(0)
				expect(counts.line):ne(0)
			end)
		end)
	end)
end)
