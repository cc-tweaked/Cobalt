local function _W(f)
	local e = setmetatable({}, { __index = getfenv() })
	setfenv(f, e)
	local r = f()
	if r ~= nil then return r end
	return e
end

bit = _W(function()
	--[[
      (c) 2008-2011 David Manura. Licensed under the same terms as Lua (MIT)
      https://github.com/davidm/lua-bit-numberlua
    ]]

	local floor = math.floor
	local MOD = 2 ^ 32
	local MODM = MOD - 1

	local function memoize(f)
		local mt = {}
		local t = setmetatable({}, mt)
		function mt:__index(k)
			local v = f(k); t[k] = v
			return v
		end

		return t
	end

	local function make_bitop_uncached(t, m)
		local function bitop(a, b)
			local res, p = 0, 1
			while a ~= 0 and b ~= 0 do
				local am, bm = a % m, b % m
				res = res + t[am][bm] * p
				a = (a - am) / m
				b = (b - bm) / m
				p = p * m
			end
			res = res + (a + b) * p
			return res
		end

		return bitop
	end

	local function make_bitop(t)
		local op1 = make_bitop_uncached(t, 2 ^ 1)
		local op2 = memoize(function(a)
			return memoize(function(b)
				return op1(a, b)
			end)
		end)
		return make_bitop_uncached(op2, 2 ^ (t.n or 1))
	end

	local bxor = make_bitop { [0] = { [0] = 0, [1] = 1 }, [1] = { [0] = 1, [1] = 0 }, n = 4 }

	local function bnot(a) return MODM - a end

	local function band(a, b) return ((a + b) - bxor(a, b)) / 2 end

	local function bor(a, b) return MODM - band(MODM - a, MODM - b) end

	local lshift, rshift -- forward declare

	local function rshift(a, disp) -- Lua5.2 insipred
	if disp < 0 then return lshift(a, -disp) end
	return floor(a % 2 ^ 32 / 2 ^ disp)
	end

	local function lshift(a, disp) -- Lua5.2 inspired
	if disp < 0 then return rshift(a, -disp) end
	return (a * 2 ^ disp) % 2 ^ 32
	end


	local function arshift(x, disp) -- Lua5.2 inspired
	local z = rshift(x, disp)
	if x >= 0x80000000 then z = z + lshift(2 ^ disp - 1, 32 - disp) end
	return z
	end

	local function bit_tobit(x)
		x = x % MOD
		if x >= 0x80000000 then x = x - MOD end
		return x
	end

	local function bit_bxor(a, b, c, ...)
		if c then
			return bit_bxor(bit_bxor(a, b), c, ...)
		elseif b then
			return bit_tobit(bxor(a % MOD, b % MOD))
		else
			return bit_tobit(a)
		end
	end


	return {
		-- bit operations
		bnot = bnot,
		band = band,
		bor = bor,
		bxor = bit_bxor,
		rshift = rshift,
		lshift = lshift,
	}
end)
gf = _W(function()
	-- finite field with base 2 and modulo irreducible polynom x^8+x^4+x^3+x+1 = 0x11d
	local bxor = bit.bxor
	local lshift = bit.lshift

	-- private data of gf
	local n = 0x100
	local ord = 0xff
	local irrPolynom = 0x11b
	local exp = {}
	local log = {}

	--
	-- add two polynoms (its simply xor)
	--
	local function add(operand1, operand2)
		return bxor(operand1, operand2)
	end

	--
	-- subtract two polynoms (same as addition)
	--
	local function sub(operand1, operand2)
		return bxor(operand1, operand2)
	end

	--
	-- inverts element
	-- a^(-1) = g^(order - log(a))
	--
	local function invert(operand)
		-- special case for 1
		if (operand == 1) then
			return 1
		end
		-- normal invert
		local exponent = ord - log[operand]
		return exp[exponent]
	end

	--
	-- multiply two elements using a logarithm table
	-- a*b = g^(log(a)+log(b))
	--
	local function mul(operand1, operand2)
		if (operand1 == 0 or operand2 == 0) then
			return 0
		end

		local exponent = log[operand1] + log[operand2]
		if (exponent >= ord) then
			exponent = exponent - ord
		end
		return exp[exponent]
	end

	--
	-- divide two elements
	-- a/b = g^(log(a)-log(b))
	--
	local function div(operand1, operand2)
		if (operand1 == 0) then
			return 0
		end
		-- TODO: exception if operand2 == 0
		local exponent = log[operand1] - log[operand2]
		if (exponent < 0) then
			exponent = exponent + ord
		end
		return exp[exponent]
	end

	--
	-- print logarithmic table
	--
	local function printLog()
		for i = 1, n do
			print("log(", i - 1, ")=", log[i - 1])
		end
	end

	--
	-- print exponentiation table
	--
	local function printExp()
		for i = 1, n do
			print("exp(", i - 1, ")=", exp[i - 1])
		end
	end

	--
	-- calculate logarithmic and exponentiation table
	--
	local function initMulTable()
		local a = 1

		for i = 0, ord - 1 do
			exp[i] = a
			log[a] = i

			-- multiply with generator x+1 -> left shift + 1
			a = bxor(lshift(a, 1), a)

			-- if a gets larger than order, reduce modulo irreducible polynom
			if a > ord then
				a = sub(a, irrPolynom)
			end
		end
	end

	initMulTable()

	return {
		add = add,
		sub = sub,
		invert = invert,
		mul = mul,
		div = dib,
		printLog = printLog,
		printExp = printExp,
	}
end)
util = _W(function()
	-- Cache some bit operators
	local bxor = bit.bxor
	local rshift = bit.rshift
	local band = bit.band
	local lshift = bit.lshift

	local sleepCheckIn
	--
	-- calculate the parity of one byte
	--
	local function byteParity(byte)
		byte = bxor(byte, rshift(byte, 4))
		byte = bxor(byte, rshift(byte, 2))
		byte = bxor(byte, rshift(byte, 1))
		return band(byte, 1)
	end

	--
	-- get byte at position index
	--
	local function getByte(number, index)
		if (index == 0) then
			return band(number, 0xff)
		else
			return band(rshift(number, index * 8), 0xff)
		end
	end


	--
	-- put number into int at position index
	--
	local function putByte(number, index)
		if (index == 0) then
			return band(number, 0xff)
		else
			return lshift(band(number, 0xff), index * 8)
		end
	end

	--
	-- convert byte array to int array
	--
	local function bytesToInts(bytes, start, n)
		local ints = {}
		for i = 0, n - 1 do
			ints[i] = putByte(bytes[start + (i * 4)], 3)
				+ putByte(bytes[start + (i * 4) + 1], 2)
				+ putByte(bytes[start + (i * 4) + 2], 1)
				+ putByte(bytes[start + (i * 4) + 3], 0)

			if n % 10000 == 0 then sleepCheckIn() end
		end
		return ints
	end

	--
	-- convert int array to byte array
	--
	local function intsToBytes(ints, output, outputOffset, n)
		n = n or #ints
		for i = 0, n do
			for j = 0, 3 do
				output[outputOffset + i * 4 + (3 - j)] = getByte(ints[i], j)
			end

			if n % 10000 == 0 then sleepCheckIn() end
		end
		return output
	end

	--
	-- convert bytes to hexString
	--
	local function bytesToHex(bytes)
		local hexBytes = ""

		for i, byte in ipairs(bytes) do
			hexBytes = hexBytes .. string.format("%02x ", byte)
		end

		return hexBytes
	end

	--
	-- convert data to hex string
	--
	local function toHexString(data)
		local type = type(data)
		if (type == "number") then
			return string.format("%08x", data)
		elseif (type == "table") then
			return bytesToHex(data)
		elseif (type == "string") then
			local bytes = { string.byte(data, 1, #data) }

			return bytesToHex(bytes)
		else
			return data
		end
	end

	local function padByteString(data)
		local dataLength = #data

		local random1 = math.random(0, 255)
		local random2 = math.random(0, 255)

		local prefix = string.char(random1,
			random2,
			random1,
			random2,
			getByte(dataLength, 3),
			getByte(dataLength, 2),
			getByte(dataLength, 1),
			getByte(dataLength, 0))

		data = prefix .. data

		local paddingLength = math.ceil(#data / 16) * 16 - #data
		local padding = ""
		for i = 1, paddingLength do
			padding = padding .. string.char(math.random(0, 255))
		end

		return data .. padding
	end

	local function properlyDecrypted(data)
		local random = { string.byte(data, 1, 4) }

		if (random[1] == random[3] and random[2] == random[4]) then
			return true
		end

		return false
	end

	local function unpadByteString(data)
		if (not properlyDecrypted(data)) then
			return nil
		end

		local dataLength = putByte(string.byte(data, 5), 3)
			+ putByte(string.byte(data, 6), 2)
			+ putByte(string.byte(data, 7), 1)
			+ putByte(string.byte(data, 8), 0)

		return string.sub(data, 9, 8 + dataLength)
	end

	local function xorIV(data, iv)
		for i = 1, 16 do
			data[i] = bxor(data[i], iv[i])
		end
	end

	local function sleepCheckIn() end

	local function getRandomData(bytes)
		local char, random, sleep, insert = string.char, math.random, sleepCheckIn, table.insert
		local result = {}

		for i = 1, bytes do
			insert(result, random(0, 255))
			if i % 10240 == 0 then sleep() end
		end

		return result
	end

	return {
		byteParity = byteParity,
		getByte = getByte,
		putByte = putByte,
		bytesToInts = bytesToInts,
		intsToBytes = intsToBytes,
		bytesToHex = bytesToHex,
		toHexString = toHexString,
		padByteString = padByteString,
		properlyDecrypted = properlyDecrypted,
		unpadByteString = unpadByteString,
		xorIV = xorIV,
		sleepCheckIn = sleepCheckIn,
		getRandomData = getRandomData,
	}
end)
aes = _W(function()
	-- Implementation of AES with nearly pure lua
	-- AES with lua is slow, really slow :-)

	local putByte = util.putByte
	local getByte = util.getByte

	-- some constants
	local ROUNDS = 'rounds'
	local KEY_TYPE = "type"
	local ENCRYPTION_KEY = 1
	local DECRYPTION_KEY = 2

	-- aes SBOX
	local SBox = {}
	local iSBox = {}

	-- aes tables
	local table0 = {}
	local table1 = {}
	local table2 = {}
	local table3 = {}

	local tableInv0 = {}
	local tableInv1 = {}
	local tableInv2 = {}
	local tableInv3 = {}

	-- round constants
	local rCon = {
		0x01000000,
		0x02000000,
		0x04000000,
		0x08000000,
		0x10000000,
		0x20000000,
		0x40000000,
		0x80000000,
		0x1b000000,
		0x36000000,
		0x6c000000,
		0xd8000000,
		0xab000000,
		0x4d000000,
		0x9a000000,
		0x2f000000,
	}

	--
	-- affine transformation for calculating the S-Box of AES
	--
	local function affinMap(byte)
		mask = 0xf8
		result = 0
		for i = 1, 8 do
			result = bit.lshift(result, 1)

			parity = util.byteParity(bit.band(byte, mask))
			result = result + parity

			-- simulate roll
			lastbit = bit.band(mask, 1)
			mask = bit.band(bit.rshift(mask, 1), 0xff)
			if (lastbit ~= 0) then
				mask = bit.bor(mask, 0x80)
			else
				mask = bit.band(mask, 0x7f)
			end
		end

		return bit.bxor(result, 0x63)
	end

	--
	-- calculate S-Box and inverse S-Box of AES
	-- apply affine transformation to inverse in finite field 2^8
	--
	local function calcSBox()
		for i = 0, 255 do
			if (i ~= 0) then
				inverse = gf.invert(i)
			else
				inverse = i
			end
			mapped = affinMap(inverse)
			SBox[i] = mapped
			iSBox[mapped] = i
		end
	end

	--
	-- Calculate round tables
	-- round tables are used to calculate shiftRow, MixColumn and SubBytes
	-- with 4 table lookups and 4 xor operations.
	--
	local function calcRoundTables()
		for x = 0, 255 do
			byte = SBox[x]
			table0[x] = putByte(gf.mul(0x03, byte), 0)
				+ putByte(byte, 1)
				+ putByte(byte, 2)
				+ putByte(gf.mul(0x02, byte), 3)
			table1[x] = putByte(byte, 0)
				+ putByte(byte, 1)
				+ putByte(gf.mul(0x02, byte), 2)
				+ putByte(gf.mul(0x03, byte), 3)
			table2[x] = putByte(byte, 0)
				+ putByte(gf.mul(0x02, byte), 1)
				+ putByte(gf.mul(0x03, byte), 2)
				+ putByte(byte, 3)
			table3[x] = putByte(gf.mul(0x02, byte), 0)
				+ putByte(gf.mul(0x03, byte), 1)
				+ putByte(byte, 2)
				+ putByte(byte, 3)
		end
	end

	--
	-- Calculate inverse round tables
	-- does the inverse of the normal roundtables for the equivalent
	-- decryption algorithm.
	--
	local function calcInvRoundTables()
		for x = 0, 255 do
			byte = iSBox[x]
			tableInv0[x] = putByte(gf.mul(0x0b, byte), 0)
				+ putByte(gf.mul(0x0d, byte), 1)
				+ putByte(gf.mul(0x09, byte), 2)
				+ putByte(gf.mul(0x0e, byte), 3)
			tableInv1[x] = putByte(gf.mul(0x0d, byte), 0)
				+ putByte(gf.mul(0x09, byte), 1)
				+ putByte(gf.mul(0x0e, byte), 2)
				+ putByte(gf.mul(0x0b, byte), 3)
			tableInv2[x] = putByte(gf.mul(0x09, byte), 0)
				+ putByte(gf.mul(0x0e, byte), 1)
				+ putByte(gf.mul(0x0b, byte), 2)
				+ putByte(gf.mul(0x0d, byte), 3)
			tableInv3[x] = putByte(gf.mul(0x0e, byte), 0)
				+ putByte(gf.mul(0x0b, byte), 1)
				+ putByte(gf.mul(0x0d, byte), 2)
				+ putByte(gf.mul(0x09, byte), 3)
		end
	end


	--
	-- rotate word: 0xaabbccdd gets 0xbbccddaa
	-- used for key schedule
	--
	local function rotWord(word)
		local tmp = bit.band(word, 0xff000000)
		return (bit.lshift(word, 8) + bit.rshift(tmp, 24))
	end

	--
	-- replace all bytes in a word with the SBox.
	-- used for key schedule
	--
	local function subWord(word)
		return putByte(SBox[getByte(word, 0)], 0)
			+ putByte(SBox[getByte(word, 1)], 1)
			+ putByte(SBox[getByte(word, 2)], 2)
			+ putByte(SBox[getByte(word, 3)], 3)
	end

	--
	-- generate key schedule for aes encryption
	--
	-- returns table with all round keys and
	-- the necessary number of rounds saved in [ROUNDS]
	--
	local function expandEncryptionKey(key)
		local keySchedule = {}
		local keyWords = math.floor(#key / 4)


		if ((keyWords ~= 4 and keyWords ~= 6 and keyWords ~= 8) or (keyWords * 4 ~= #key)) then
			print("Invalid key size: ", keyWords)
			return nil
		end

		keySchedule[ROUNDS] = keyWords + 6
		keySchedule[KEY_TYPE] = ENCRYPTION_KEY

		for i = 0, keyWords - 1 do
			keySchedule[i] = putByte(key[i * 4 + 1], 3)
				+ putByte(key[i * 4 + 2], 2)
				+ putByte(key[i * 4 + 3], 1)
				+ putByte(key[i * 4 + 4], 0)
		end

		for i = keyWords, (keySchedule[ROUNDS] + 1) * 4 - 1 do
			local tmp = keySchedule[i - 1]

			if (i % keyWords == 0) then
				tmp = rotWord(tmp)
				tmp = subWord(tmp)

				local index = math.floor(i / keyWords)
				tmp = bit.bxor(tmp, rCon[index])
			elseif (keyWords > 6 and i % keyWords == 4) then
				tmp = subWord(tmp)
			end

			keySchedule[i] = bit.bxor(keySchedule[(i - keyWords)], tmp)
		end

		return keySchedule
	end

	--
	-- Inverse mix column
	-- used for key schedule of decryption key
	--
	local function invMixColumnOld(word)
		local b0 = getByte(word, 3)
		local b1 = getByte(word, 2)
		local b2 = getByte(word, 1)
		local b3 = getByte(word, 0)

		return putByte(gf.add(gf.add(gf.add(gf.mul(0x0b, b1),
			gf.mul(0x0d, b2)),
			gf.mul(0x09, b3)),
			gf.mul(0x0e, b0)), 3)
			+ putByte(gf.add(gf.add(gf.add(gf.mul(0x0b, b2),
			gf.mul(0x0d, b3)),
			gf.mul(0x09, b0)),
			gf.mul(0x0e, b1)), 2)
			+ putByte(gf.add(gf.add(gf.add(gf.mul(0x0b, b3),
			gf.mul(0x0d, b0)),
			gf.mul(0x09, b1)),
			gf.mul(0x0e, b2)), 1)
			+ putByte(gf.add(gf.add(gf.add(gf.mul(0x0b, b0),
			gf.mul(0x0d, b1)),
			gf.mul(0x09, b2)),
			gf.mul(0x0e, b3)), 0)
	end

	--
	-- Optimized inverse mix column
	-- look at http://fp.gladman.plus.com/cryptography_technology/rijndael/aes.spec.311.pdf
	-- TODO: make it work
	--
	local function invMixColumn(word)
		local b0 = getByte(word, 3)
		local b1 = getByte(word, 2)
		local b2 = getByte(word, 1)
		local b3 = getByte(word, 0)

		local t = bit.bxor(b3, b2)
		local u = bit.bxor(b1, b0)
		local v = bit.bxor(t, u)
		v = bit.bxor(v, gf.mul(0x08, v))
		w = bit.bxor(v, gf.mul(0x04, bit.bxor(b2, b0)))
		v = bit.bxor(v, gf.mul(0x04, bit.bxor(b3, b1)))

		return putByte(bit.bxor(bit.bxor(b3, v), gf.mul(0x02, bit.bxor(b0, b3))), 0)
			+ putByte(bit.bxor(bit.bxor(b2, w), gf.mul(0x02, t)), 1)
			+ putByte(bit.bxor(bit.bxor(b1, v), gf.mul(0x02, bit.bxor(b0, b3))), 2)
			+ putByte(bit.bxor(bit.bxor(b0, w), gf.mul(0x02, u)), 3)
	end

	--
	-- generate key schedule for aes decryption
	--
	-- uses key schedule for aes encryption and transforms each
	-- key by inverse mix column.
	--
	local function expandDecryptionKey(key)
		local keySchedule = expandEncryptionKey(key)
		if (keySchedule == nil) then
			return nil
		end

		keySchedule[KEY_TYPE] = DECRYPTION_KEY

		for i = 4, (keySchedule[ROUNDS] + 1) * 4 - 5 do
			keySchedule[i] = invMixColumnOld(keySchedule[i])
		end

		return keySchedule
	end

	--
	-- xor round key to state
	--
	local function addRoundKey(state, key, round)
		for i = 0, 3 do
			state[i] = bit.bxor(state[i], key[round * 4 + i])
		end
	end

	--
	-- do encryption round (ShiftRow, SubBytes, MixColumn together)
	--
	local function doRound(origState, dstState)
		dstState[0] = bit.bxor(bit.bxor(bit.bxor(table0[getByte(origState[0], 3)],
			table1[getByte(origState[1], 2)]),
			table2[getByte(origState[2], 1)]),
			table3[getByte(origState[3], 0)])

		dstState[1] = bit.bxor(bit.bxor(bit.bxor(table0[getByte(origState[1], 3)],
			table1[getByte(origState[2], 2)]),
			table2[getByte(origState[3], 1)]),
			table3[getByte(origState[0], 0)])

		dstState[2] = bit.bxor(bit.bxor(bit.bxor(table0[getByte(origState[2], 3)],
			table1[getByte(origState[3], 2)]),
			table2[getByte(origState[0], 1)]),
			table3[getByte(origState[1], 0)])

		dstState[3] = bit.bxor(bit.bxor(bit.bxor(table0[getByte(origState[3], 3)],
			table1[getByte(origState[0], 2)]),
			table2[getByte(origState[1], 1)]),
			table3[getByte(origState[2], 0)])
	end

	--
	-- do last encryption round (ShiftRow and SubBytes)
	--
	local function doLastRound(origState, dstState)
		dstState[0] = putByte(SBox[getByte(origState[0], 3)], 3)
			+ putByte(SBox[getByte(origState[1], 2)], 2)
			+ putByte(SBox[getByte(origState[2], 1)], 1)
			+ putByte(SBox[getByte(origState[3], 0)], 0)

		dstState[1] = putByte(SBox[getByte(origState[1], 3)], 3)
			+ putByte(SBox[getByte(origState[2], 2)], 2)
			+ putByte(SBox[getByte(origState[3], 1)], 1)
			+ putByte(SBox[getByte(origState[0], 0)], 0)

		dstState[2] = putByte(SBox[getByte(origState[2], 3)], 3)
			+ putByte(SBox[getByte(origState[3], 2)], 2)
			+ putByte(SBox[getByte(origState[0], 1)], 1)
			+ putByte(SBox[getByte(origState[1], 0)], 0)

		dstState[3] = putByte(SBox[getByte(origState[3], 3)], 3)
			+ putByte(SBox[getByte(origState[0], 2)], 2)
			+ putByte(SBox[getByte(origState[1], 1)], 1)
			+ putByte(SBox[getByte(origState[2], 0)], 0)
	end

	--
	-- do decryption round
	--
	local function doInvRound(origState, dstState)
		dstState[0] = bit.bxor(bit.bxor(bit.bxor(tableInv0[getByte(origState[0], 3)],
			tableInv1[getByte(origState[3], 2)]),
			tableInv2[getByte(origState[2], 1)]),
			tableInv3[getByte(origState[1], 0)])

		dstState[1] = bit.bxor(bit.bxor(bit.bxor(tableInv0[getByte(origState[1], 3)],
			tableInv1[getByte(origState[0], 2)]),
			tableInv2[getByte(origState[3], 1)]),
			tableInv3[getByte(origState[2], 0)])

		dstState[2] = bit.bxor(bit.bxor(bit.bxor(tableInv0[getByte(origState[2], 3)],
			tableInv1[getByte(origState[1], 2)]),
			tableInv2[getByte(origState[0], 1)]),
			tableInv3[getByte(origState[3], 0)])

		dstState[3] = bit.bxor(bit.bxor(bit.bxor(tableInv0[getByte(origState[3], 3)],
			tableInv1[getByte(origState[2], 2)]),
			tableInv2[getByte(origState[1], 1)]),
			tableInv3[getByte(origState[0], 0)])
	end

	--
	-- do last decryption round
	--
	local function doInvLastRound(origState, dstState)
		dstState[0] = putByte(iSBox[getByte(origState[0], 3)], 3)
			+ putByte(iSBox[getByte(origState[3], 2)], 2)
			+ putByte(iSBox[getByte(origState[2], 1)], 1)
			+ putByte(iSBox[getByte(origState[1], 0)], 0)

		dstState[1] = putByte(iSBox[getByte(origState[1], 3)], 3)
			+ putByte(iSBox[getByte(origState[0], 2)], 2)
			+ putByte(iSBox[getByte(origState[3], 1)], 1)
			+ putByte(iSBox[getByte(origState[2], 0)], 0)

		dstState[2] = putByte(iSBox[getByte(origState[2], 3)], 3)
			+ putByte(iSBox[getByte(origState[1], 2)], 2)
			+ putByte(iSBox[getByte(origState[0], 1)], 1)
			+ putByte(iSBox[getByte(origState[3], 0)], 0)

		dstState[3] = putByte(iSBox[getByte(origState[3], 3)], 3)
			+ putByte(iSBox[getByte(origState[2], 2)], 2)
			+ putByte(iSBox[getByte(origState[1], 1)], 1)
			+ putByte(iSBox[getByte(origState[0], 0)], 0)
	end

	--
	-- encrypts 16 Bytes
	-- key           encryption key schedule
	-- input         array with input data
	-- inputOffset   start index for input
	-- output        array for encrypted data
	-- outputOffset  start index for output
	--
	local function encrypt(key, input, inputOffset, output, outputOffset)
		--default parameters
		inputOffset = inputOffset or 1
		output = output or {}
		outputOffset = outputOffset or 1

		local state = {}
		local tmpState = {}

		if (key[KEY_TYPE] ~= ENCRYPTION_KEY) then
			print("No encryption key: ", key[KEY_TYPE])
			return
		end

		state = util.bytesToInts(input, inputOffset, 4)
		addRoundKey(state, key, 0)

		local checkIn = util.sleepCheckIn

		local round = 1
		while (round < key[ROUNDS] - 1) do
			-- do a double round to save temporary assignments
			doRound(state, tmpState)
			addRoundKey(tmpState, key, round)
			round = round + 1

			doRound(tmpState, state)
			addRoundKey(state, key, round)
			round = round + 1
		end

		checkIn()

		doRound(state, tmpState)
		addRoundKey(tmpState, key, round)
		round = round + 1

		doLastRound(tmpState, state)
		addRoundKey(state, key, round)

		return util.intsToBytes(state, output, outputOffset)
	end

	--
	-- decrypt 16 bytes
	-- key           decryption key schedule
	-- input         array with input data
	-- inputOffset   start index for input
	-- output        array for decrypted data
	-- outputOffset  start index for output
	---
	local function decrypt(key, input, inputOffset, output, outputOffset)
		-- default arguments
		inputOffset = inputOffset or 1
		output = output or {}
		outputOffset = outputOffset or 1

		local state = {}
		local tmpState = {}

		if (key[KEY_TYPE] ~= DECRYPTION_KEY) then
			print("No decryption key: ", key[KEY_TYPE])
			return
		end

		state = util.bytesToInts(input, inputOffset, 4)
		addRoundKey(state, key, key[ROUNDS])

		local checkIn = util.sleepCheckIn

		local round = key[ROUNDS] - 1
		while (round > 2) do
			-- do a double round to save temporary assignments
			doInvRound(state, tmpState)
			addRoundKey(tmpState, key, round)
			round = round - 1

			doInvRound(tmpState, state)
			addRoundKey(state, key, round)
			round = round - 1

			if round % 32 == 0 then
				checkIn()
			end
		end

		checkIn()

		doInvRound(state, tmpState)
		addRoundKey(tmpState, key, round)
		round = round - 1

		doInvLastRound(tmpState, state)
		addRoundKey(state, key, round)

		return util.intsToBytes(state, output, outputOffset)
	end

	-- calculate all tables when loading this file
	calcSBox()
	calcRoundTables()
	calcInvRoundTables()

	return {
		ROUNDS = ROUNDS,
		KEY_TYPE = KEY_TYPE,
		ENCRYPTION_KEY = ENCRYPTION_KEY,
		DECRYPTION_KEY = DECRYPTION_KEY,
		expandEncryptionKey = expandEncryptionKey,
		expandDecryptionKey = expandDecryptionKey,
		encrypt = encrypt,
		decrypt = decrypt,
	}
end)
buffer = _W(function()
	local function new()
		return {}
	end

	local function addString(stack, s)
		table.insert(stack, s)
		for i = #stack - 1, 1, -1 do
			if #stack[i] > #stack[i + 1] then
				break
			end
			stack[i] = stack[i] .. table.remove(stack)
		end
	end

	local function toString(stack)
		for i = #stack - 1, 1, -1 do
			stack[i] = stack[i] .. table.remove(stack)
		end
		return stack[1]
	end

	return {
		new = new,
		addString = addString,
		toString = toString,
	}
end)
ciphermode = _W(function()
	local public = {}

	--
	-- Encrypt strings
	-- key - byte array with key
	-- string - string to encrypt
	-- modefunction - function for cipher mode to use
	--
	function public.encryptString(key, data, modeFunction)
		local iv = iv or { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }
		local keySched = aes.expandEncryptionKey(key)
		local encryptedData = buffer.new()

		for i = 1, #data / 16 do
			local offset = (i - 1) * 16 + 1
			local byteData = { string.byte(data, offset, offset + 15) }

			modeFunction(keySched, byteData, iv)

			buffer.addString(encryptedData, string.char(unpack(byteData)))
		end

		return buffer.toString(encryptedData)
	end

	--
	-- the following 4 functions can be used as
	-- modefunction for encryptString
	--

	-- Electronic code book mode encrypt function
	function public.encryptECB(keySched, byteData, iv)
		aes.encrypt(keySched, byteData, 1, byteData, 1)
	end

	-- Cipher block chaining mode encrypt function
	function public.encryptCBC(keySched, byteData, iv)
		util.xorIV(byteData, iv)

		aes.encrypt(keySched, byteData, 1, byteData, 1)

		for j = 1, 16 do
			iv[j] = byteData[j]
		end
	end

	-- Output feedback mode encrypt function
	function public.encryptOFB(keySched, byteData, iv)
		aes.encrypt(keySched, iv, 1, iv, 1)
		util.xorIV(byteData, iv)
	end

	-- Cipher feedback mode encrypt function
	function public.encryptCFB(keySched, byteData, iv)
		aes.encrypt(keySched, iv, 1, iv, 1)
		util.xorIV(byteData, iv)

		for j = 1, 16 do
			iv[j] = byteData[j]
		end
	end

	--
	-- Decrypt strings
	-- key - byte array with key
	-- string - string to decrypt
	-- modefunction - function for cipher mode to use
	--
	function public.decryptString(key, data, modeFunction)
		local iv = iv or { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }

		local keySched
		if (modeFunction == public.decryptOFB or modeFunction == public.decryptCFB) then
			keySched = aes.expandEncryptionKey(key)
		else
			keySched = aes.expandDecryptionKey(key)
		end

		local decryptedData = buffer.new()

		for i = 1, #data / 16 do
			local offset = (i - 1) * 16 + 1
			local byteData = { string.byte(data, offset, offset + 15) }

			iv = modeFunction(keySched, byteData, iv)

			buffer.addString(decryptedData, string.char(unpack(byteData)))
		end

		return buffer.toString(decryptedData)
	end

	--
	-- the following 4 functions can be used as
	-- modefunction for decryptString
	--

	-- Electronic code book mode decrypt function
	function public.decryptECB(keySched, byteData, iv)

		aes.decrypt(keySched, byteData, 1, byteData, 1)

		return iv
	end

	-- Cipher block chaining mode decrypt function
	function public.decryptCBC(keySched, byteData, iv)
		local nextIV = {}
		for j = 1, 16 do
			nextIV[j] = byteData[j]
		end

		aes.decrypt(keySched, byteData, 1, byteData, 1)
		util.xorIV(byteData, iv)

		return nextIV
	end

	-- Output feedback mode decrypt function
	function public.decryptOFB(keySched, byteData, iv)
		aes.encrypt(keySched, iv, 1, iv, 1)
		util.xorIV(byteData, iv)

		return iv
	end

	-- Cipher feedback mode decrypt function
	function public.decryptCFB(keySched, byteData, iv)
		local nextIV = {}
		for j = 1, 16 do
			nextIV[j] = byteData[j]
		end

		aes.encrypt(keySched, iv, 1, iv, 1)

		util.xorIV(byteData, iv)

		return nextIV
	end

	return public
end)
--@require lib/ciphermode.lua
--@require lib/util.lua
--
-- Simple API for encrypting strings.
--
AES128 = 16
AES192 = 24
AES256 = 32

ECBMODE = 1
CBCMODE = 2
OFBMODE = 3
CFBMODE = 4

local function pwToKey(password, keyLength)
	local padLength = keyLength
	if (keyLength == AES192) then
		padLength = 32
	end

	if (padLength > #password) then
		local postfix = ""
		for i = 1, padLength - #password do
			postfix = postfix .. string.char(0)
		end
		password = password .. postfix
	else
		password = string.sub(password, 1, padLength)
	end

	local pwBytes = { string.byte(password, 1, #password) }
	password = ciphermode.encryptString(pwBytes, password, ciphermode.encryptCBC)

	password = string.sub(password, 1, keyLength)

	return { string.byte(password, 1, #password) }
end

--
-- Encrypts string data with password password.
-- password  - the encryption key is generated from this string
-- data      - string to encrypt (must not be too large)
-- keyLength - length of aes key: 128(default), 192 or 256 Bit
-- mode      - mode of encryption: ecb, cbc(default), ofb, cfb
--
-- mode and keyLength must be the same for encryption and decryption.
--
function encrypt(password, data, keyLength, mode)
	assert(password ~= nil, "Empty password.")
	assert(password ~= nil, "Empty data.")

	local mode = mode or CBCMODE
	local keyLength = keyLength or AES128

	local key = pwToKey(password, keyLength)

	local paddedData = util.padByteString(data)

	if (mode == ECBMODE) then
		return ciphermode.encryptString(key, paddedData, ciphermode.encryptECB)
	elseif (mode == CBCMODE) then
		return ciphermode.encryptString(key, paddedData, ciphermode.encryptCBC)
	elseif (mode == OFBMODE) then
		return ciphermode.encryptString(key, paddedData, ciphermode.encryptOFB)
	elseif (mode == CFBMODE) then
		return ciphermode.encryptString(key, paddedData, ciphermode.encryptCFB)
	else
		return nil
	end
end




--
-- Decrypts string data with password password.
-- password  - the decryption key is generated from this string
-- data      - string to encrypt
-- keyLength - length of aes key: 128(default), 192 or 256 Bit
-- mode      - mode of decryption: ecb, cbc(default), ofb, cfb
--
-- mode and keyLength must be the same for encryption and decryption.
--
function decrypt(password, data, keyLength, mode)
	local mode = mode or CBCMODE
	local keyLength = keyLength or AES128

	local key = pwToKey(password, keyLength)

	local plain
	if (mode == ECBMODE) then
		plain = ciphermode.decryptString(key, data, ciphermode.decryptECB)
	elseif (mode == CBCMODE) then
		plain = ciphermode.decryptString(key, data, ciphermode.decryptCBC)
	elseif (mode == OFBMODE) then
		plain = ciphermode.decryptString(key, data, ciphermode.decryptOFB)
	elseif (mode == CFBMODE) then
		plain = ciphermode.decryptString(key, data, ciphermode.decryptCFB)
	end

	result = util.unpadByteString(plain)

	if (result == nil) then
		return nil
	end

	return result
end
