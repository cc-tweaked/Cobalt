local a, b = 1, 2

suspend()

assert(a + b == 3)
return "OK"
