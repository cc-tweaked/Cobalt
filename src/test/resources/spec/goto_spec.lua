describe("goto statements :lua>=5.2", function()
	describe("parse failures", function()
		local function fail_parse(code, err)
			local st, msg = load(code, "=in")
			expect(st):describe("Resulting function"):eq(nil)
			expect(msg):str_match(err)
		end

		it("cannot see label inside block", function()
			fail_parse([[ goto l1; do ::l1:: end ]], "label 'l1'")
			fail_parse([[
				do ::l1:: end
				goto l1
			]], "no visible label 'l1' for <goto> at line 2")
		end)

		it("repeated label", function()
			fail_parse([[ ::l1:: ::l1:: ]], "label 'l1' already defined")
		end)

		it("repeated label in different blocks :lua>=5.4", function()
			fail_parse([[::l1:: do ::l1:: end]], "label 'l1' already defined")
		end)

		it("undefined label", function()
			fail_parse([[ goto l1; local aa ::l1:: ::l2:: print(3) ]], "into the scope of local 'aa'")
		end)

		it("jumping over variable definition", function()
			fail_parse([[
			do local bb, cc; goto l1; end
			local aa
			::l1:: print(3)
			]], "into the scope of local 'aa'")
		end)

		it("jumping into a block", function()
			fail_parse([[ do ::l1:: end goto l1 ]], "label 'l1'")
			fail_parse([[ goto l1 do ::l1:: end ]], "label 'l1'")
		end)

		it("within a repeat until", function()
			fail_parse([[
				repeat
					if x then goto cont end
					local xuxu = 10
					::cont::
				until xuxu < x
			]], "local 'xuxu'")
		end)

		it("break outside a loop :lua>=5.4", function()
			fail_parse([[
				do end
				break
			]], "break outside loop at line 2")
		end)
	end)

	it("a simple goto", function()
		local prog = [[
		local x
		do
			local y = 12
			goto l1
			::l2:: x = x + 1; goto l3
			::l1:: x = y; goto l2
		end
		::l3:: ::l3_1::
		expect(x):eq(13)
		]]
		assert(load(prog))()
	end)

	it("long labels", function()
		local prog = [[
		do
			local a = 1
			goto l%sa; a = a + 1
			::l%sa:: a = a + 10
			goto l%sb; a = a + 2
			::l%sb:: a = a + 20
			return a
		end
		]]
		local label = string.rep("0123456789", 40)
		prog = string.format(prog, label, label, label, label)
		expect(assert(load(prog))()):eq(31)
	end)

	local function it_str(name, prog)
		it(name, function() assert(load(prog))() end)
	end

	-- This fails on Lua 5.4 as labels cannot repeat.
	it_str("goes to nearest label :lua<5.4 :!cobalt", [[
		::l3::
		-- goto to correct label when nested
		do goto l3; ::l3:: end   -- does not loop jumping to previous label 'l3'
	]])

	describe("may jump over local declaration", function()
		it_str("in do blocks", [[
			local x = 0
			do
				goto l1
				local a = 23
				x = a
				::l1::;
			end
			expect(x):eq(0)
		]])

		it_str("in while loops", [[
		local x = 13
		while true do
			goto l4
			goto l1  -- ok to jump over local dec. to end of block
			goto l1  -- multiple uses of same label
			local x = 45
			::l1:: ;;;
		end
		::l4::
		expect(x):eq(13)
		]])

		it_str("in if blocks", [[
			if print then
				goto l1   -- ok to jump over local dec. to end of block
				error("should not be here")
				goto l2   -- ok to jump over local dec. to end of block
				local x
				::l1:: ; ::l2:: ;;
			else end
		]])
	end)

	it_str("may repeat labels in nested functions", [[
		local function foo ()
			local a = {}
			goto l3
			::l1:: a[#a + 1] = 1; goto l2;
			::l2:: a[#a + 1] = 2; goto l5;
			::l3::
			::l3a:: a[#a + 1] = 3; goto l1;
			::l4:: a[#a + 1] = 4; goto l6;
			::l5:: a[#a + 1] = 5; goto l4;
			::l6::
			expect(a[1]):eq(3) expect(a[2]):eq(1) expect(a[3]):eq(2) expect(a[4]):eq(5) expect(a[5]):eq(4)
			if not a[6] then a[6] = true; goto l3a end   -- do it twice
		end

		::l6:: foo()
	]])

	it("closing over variables", function()
		local foo = assert(load([[
			local t = {}
			do
				local i = 1
				local a, b, c, d
				t[1] = function () return a, b, c, d end
				::l1::
				local b
				do
					local c
					t[#t + 1] = function () return a, b, c, d end    -- t[2], t[4], t[6]
					if i > 2 then goto l2 end
					do
						local d
						t[#t + 1] = function () return a, b, c, d end   -- t[3], t[5]
						i = i + 1
						local a
						goto l1
					end
				end
			end
			::l2:: return t
		]]))

		local a = foo()
		expect(#a):eq(6)

		-- all functions share same 'a'
		for i = 2, 6 do
			expect(debug.upvalueid(a[1], 1)):eq(debug.upvalueid(a[i], 1))
		end

		-- 'b' and 'c' are shared among some of them
		for i = 2, 6 do
			-- only a[1] uses external 'b'/'b'
			expect(debug.upvalueid(a[1], 2)):ne(debug.upvalueid(a[i], 2))
			expect(debug.upvalueid(a[1], 3)):ne(debug.upvalueid(a[i], 3))
		end

		for i = 3, 5, 2 do
			-- inner functions share 'b'/'c' with previous ones
			expect(debug.upvalueid(a[i], 2)):eq(debug.upvalueid(a[i - 1], 2))
			expect(debug.upvalueid(a[i], 3)):eq(debug.upvalueid(a[i - 1], 3))
			-- but not with next ones
			expect(debug.upvalueid(a[i], 2)):ne(debug.upvalueid(a[i + 1], 2))
			expect(debug.upvalueid(a[i], 3)):ne(debug.upvalueid(a[i + 1], 3))
		end

		-- only external 'd' is shared
		for i = 2, 6, 2 do
			expect(debug.upvalueid(a[1], 4)):eq(debug.upvalueid(a[i], 4))
		end

		-- internal 'd's are all different
		for i = 3, 5, 2 do
			for j = 1, 6 do
				expect(debug.upvalueid(a[i], 4) == debug.upvalueid(a[j], 4)):eq(i == j)
			end
		end
	end)

	it_str("goto optimisations", [[
		local function testG (a)
			if a == 1 then
				goto l1
				error("should never be here!")
			elseif a == 2 then goto l2
			elseif a == 3 then goto l3
			elseif a == 4 then
				goto l1  -- go to inside the block
				error("should never be here!")
				::l1:: a = a + 1   -- must go to 'if' end
			else
				goto l4
				::l4a:: a = a * 2; goto l4b
				error("should never be here!")
				::l4:: goto l4a
				error("should never be here!")
				::l4b::
			end
			do return a end
			::l2:: do return "2" end
			::l3:: do return "3" end
			::l1:: return "1"
		end

		expect(testG(1)):eq("1")
		expect(testG(2)):eq("2")
		expect(testG(3)):eq("3")
		expect(testG(4)):eq(5)
		expect(testG(5)):eq(10)
	]])

	it_str("infinite loops", [[
		do
			goto escape   -- do not run the infinite loops
			::a:: goto a
			::b:: goto c
			::c:: goto b
		end
		::escape::
	]])

	it_str("Regression #1 :lua>=5.3", [[
		local x
		::L1::
		local y             -- cannot join this SETNIL with previous one
		expect(y):eq(nil)
		y = true
		if x == nil then
			x = 1
			goto L1
		else
			x = x + 1
		end
		expect(x):eq(2) expect(y):eq(true)
	]])

	it_str("Regression #2 :lua>=5.3", [[
		local first = true
		local a = false
		if true then
			goto LBL
			::loop::
			a = true
			::LBL::
			if first then
				first = false
				goto loop
			end
		end
		assert(a)
	]])

	it_str("Supports 'goto' as a normal identifier :cobalt", [[
		local function goto() end

		goto goto
		::goto::

		goto()
		print(goto)

		if true then goto() end
	]])
end)
