assert(string.char(127) >= '\0')
assert(string.char(128) >= '\0')
assert(string.char(127) >= '\127')
assert(string.char(127) <= '\128')
