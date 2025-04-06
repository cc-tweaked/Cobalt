describe("The math library", function()
	local float_bits = 53

	it("has 53 floating point bits", function()
		local computed_bits = 24
		local p = 2.0 ^ computed_bits
		while p < p + 1.0 do
			p = p * 2.0
			computed_bits = computed_bits + 1
		end
		expect(computed_bits):eq(float_bits)
	end)

	describe("math.random", function()
		it("produces an exact double :lua>=5.4", function()
			math.randomseed(1007)
			local rand = math.random()
			expect(rand):close_to(tonumber("0x0.7a7040a5a323c9d6"), 2^-float_bits)
		end)

		it("produces an exact value for integers :lua>=5.4", function()
			math.randomseed(1007)

			expect(math.random(100)):eq(87)
			expect(math.random(-10, 10)):eq(8)
			expect(math.random(-2^32, 2 ^ 32)):eq(4123808829)
		end)

		it("fails with three arguments", function()
			expect.error(math.random, 1, 2, 3):eq("wrong number of arguments")
		end)
	end)

	describe("math.atan", function()
		it("accepts two arguments :lua>=5.3", function()
			expect(math.atan(0.5, 0.5)):eq(math.pi / 4)
		end)
	end)
end)
