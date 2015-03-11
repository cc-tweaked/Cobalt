# Cobalt   [![Build Status](https://travis-ci.org/SquidDev-CC/Cobalt.svg?branch=master)](https://travis-ci.org/SquidDev-CC/Cobalt)

## What?
Cobalt is a fork of LuaJ 2.0 (Lua 5.1) with many features of LuaJ 3.0 backported.

Cobalt will have full support for Lua-to-Java bytecode compiling and ensure that these functions can be treated as normal Lua functions (string.dump, debug.getinfo, setfenv).

The other key priority is to enable multiple Lua instances to be able to run at once.
## Why?
LuaJ 2.0 had too many bugs, mostly minor but annoying. Cobalt is an attempt to slim down LuaJ (only the JSE will be supported) and fix most of the bugs.

In the end it is just an attempt to optimise as much as possible with it.

## But Lua 5.1 is outdated!
I am considering having a separate Lua 5.2/3 branch but that is not an immediate priority right now.
