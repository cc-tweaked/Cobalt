-- Miles off what you'd expect. Sorry.
print(xpcall(function()
	assert(false)
end,
	debug.traceback))

for i = 0, 3 do
	print(select(2, pcall(error, "msg", i)))
end
