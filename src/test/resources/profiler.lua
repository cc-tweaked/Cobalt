local function memorise(func)
	local cache = {}
	return function(n)
		local val = cache[n]
		if val ~= nil then return val end
		val = func(n)
		cache[n] = val
		return val
	end, cache
end

local cache = {}

local function fib(n)
	if n == 0 or n == 1 then
		return 1
	else
		--		for _ = 0, 1e6 do end -- Takes ~0.5 seconds
		return fib(n - 1) + fib(n - 2)
	end
end

--fib, cache = memorise(fib)

local function allFibs(n)
	if n == 0 then
		return fib(n)
	else
		return fib(n), allFibs(n - 1)
	end
end

local function time(label, func, ...)
	local clock = profiler.nanoTime()
	func(...)
	local took = profiler.nanoTime() - clock
	print(label .. " took " .. (took / 1e9))
end

for k, _ in pairs(cache) do cache[k] = nil end
time("Without", allFibs, 30)

io.write("> ")
io.read("*l")
profiler.start("prof.out")

for k, _ in pairs(cache) do cache[k] = nil end
time("With", allFibs, 30)

local total = profiler.stop()

local summary = {}
local file = assert(io.open("prof.out", 'rb'))


local function readByte() return file:read(1):byte() end

local function readShort()
	local a, b = file:read(2):byte(1, 2)
	return a * (2 ^ 8) + b
end

local function readInt()
	local a, b, c, d = file:read(4):byte(1, 4)
	return a * (2 ^ 24) + b * (2 ^ 16) + c * (2 ^ 8) + d
end

local function readLong()
	return readInt() * (2 ^ 32) + readInt()
end


local function readString()
	local length = readInt()
	return file:read(length)
end

local str = file:read(1)
local count = 0
while str do
	local code = str:byte()

	if code == 0x00 then
		-- Prototype
		local id = readShort()
		local name = readString()
		local line = readInt()
		local last = readInt()

		if line == last then
			name = name .. ":" .. line
		else
			name = name .. ":" .. line .. "-" .. last
		end

		summary[id] = {
			name = name,
			calls = 0,
			total = 0,
		}
	elseif code == 0x01 then
		-- Call proto
		local level = readShort()
		local id = readShort()
		local localT = readLong()
		local totalT = readLong()

		summary[id].calls = summary[id].calls + 1
		summary[id].total = summary[id].total + localT
	elseif code == 0x02 then
		local level = readShort()
		local name = readString()

		local localT = readLong()
		local totalT = readLong()

		print(name, " with ", totalT)
	else
		error("Unknown code")
	end

	count = count + 1
	if count % 10000 == 0 then
		print("Done " .. count .. "/" .. total, count / total)
	end

	str = file:read(1)
end

for _, v in pairs(summary) do
	print(v.name, "Called:" .. v.calls, "Time: " .. v.total, "Average:" .. v.total / v.calls)
end
