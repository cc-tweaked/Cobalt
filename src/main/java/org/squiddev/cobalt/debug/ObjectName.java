package org.squiddev.cobalt.debug;

import org.squiddev.cobalt.LuaString;

/**
 * The name of an object on the Lua stack.
 *
 * @param name The name of the object. This will normally the variable's (local/global/upvalue) or the field's name.
 * @param what The type of this object, such as "global" or "field".
 * @see DebugHelpers#getObjectName(DebugFrame, int)
 * @see DebugHelpers#getFuncName(DebugFrame, int)
 */
public record ObjectName(LuaString name, LuaString what) {
}
