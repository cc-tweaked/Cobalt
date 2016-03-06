local n = 1000

local key = util.getRandomData(16)
local keySched = aes.expandEncryptionKey(key)
local plaintext = util.getRandomData(16)

local encrypt = aes.encrypt

local start = os.clock()
for _ = 1, n do
	encrypt(keySched, plaintext)
end

local kByte = (n * 16) / 1024
local duration = os.clock() - start
print(string.format("\tkByte per second: %g", kByte / duration))
