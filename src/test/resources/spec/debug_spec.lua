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

		local function count_hooks(yield, ...)
			local counts = { call = 0, ['return'] = 0, count = 0, line = 0 }

			debug.sethook(function(kind, ...)
				counts[kind] = (counts[kind] or 0) + 1
				if yield then expect(coroutine.yield(kind)):eq(kind) end
			end, ...)

			expect(string.gsub("xyz", "x", "z")):eq("zyz")
			local ok = pcall(function()
				local x = 0
				for i = 1, 5 do x = x + i end
			end)
			expect(ok):eq(true)

			debug.sethook(nil)

			return counts
		end

		it("can yield within hooks :cobalt", function()
			local counts = expect.run_coroutine(function() return count_hooks(true, "crl", 1) end)

			-- These numbers are going to vary beyond the different VMs a
			-- little. As long as they're non-0, it's all fine.
			expect(counts.call):ne(0)
			expect(counts['return']):ne(0)
			expect(counts.count):ne(0)
			expect(counts.line):ne(0)
		end)

		it("counts calls, returns and lines :lua==5.2", function()
			local counts = count_hooks(false, "crl")

			expect(counts.call):eq(10)
			expect(counts['return']):eq(10)
			expect(counts.count):eq(0)
			expect(counts.line):eq(20)
		end)

		it("line hooks are triggered for each instruction :lua==5.2", function()
			local counts = count_hooks(false, "crl", 1)

			expect(counts.call):eq(10)
			expect(counts['return']):eq(10)
			expect(counts.count):ne(0)
			expect(counts.line):eq(59)
		end)
	end)

	describe("debug.getinfo", function()
		it("a native function :lua>=5.3", function()
			expect(debug.getinfo(print)):matches {
				nups = 0,
				currentline = -1,
				func = print,
				istailcall = false,
				isvararg = true,
				lastlinedefined = -1,
				linedefined = -1,
				namewhat = "",
				nparams = 0,
				short_src = "[C]",
				source = "=[C]",
				what = "C",
			}
		end)
	end)
end)
