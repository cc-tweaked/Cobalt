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
	end)
end)
