# Cobalt
Cobalt is a Lua implementation for Java, designed for use in the Minecraft mod
[CC: Tweaked]. It is based on [LuaJ 2.0][LuaJ], though has diverged
significantly over the years.

## Features
Cobalt is a (mostly) compliant implementation of Lua 5.2, with several
interesting additional features:

 - Backwards compatibility with Lua 5.1 (`getfenv`/`setfenv`, `goto` as an
   identifier).
 - Backports much of the Lua 5.3 and 5.4 standard library.
 - Allows yielding _anywhere_ within a Lua program, including debug hooks and
   inside any native function.
 - Support for interrupting and resuming the VM at arbitrary points.
 - Efficient concatenation of strings using ropes.

## Using
Don't.

No seriously, don't. Cobalt is developed in-sync with CC: Tweaked, and so grows
and changes according to the mod's needs. There is no guarantee of API stability
between versions. It makes many design decisions which make sense for CC, but
not for anything which needs a normal Lua implementation.

Instead I recommend using one of the alternative Lua implementations, like
LuaJ, JNLua or Rembulan.

[CC: Tweaked]: https://github.com/cc-tweaked/CC-Tweaked "cc-tweaked/CC-Tweaked: Just another ComputerCraft fork"
[LuaJ]: https://github.com/luaj/luaj "luaj/luaj: Lightweight, fast, Java-centric Lua interpreter written for JME and JSE."
