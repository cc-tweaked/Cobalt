local function fails(str, exp)
	local ok, err = (loadstring or load)(str, "=x")
	assert(not ok)
	if err ~= exp then error(("Failure:\nExpected '%s',\n     got '%s'"):format(exp, err), 2) end
end

fails("return )", "x:1: unexpected symbol near ')'")
fails("abc", "x:1: syntax error near <eof>")
fails("abc =", "x:1: unexpected symbol near <eof>")
