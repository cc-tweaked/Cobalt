package cc.squiddev.cobalt.build.coroutine

import org.objectweb.asm.*

/** A descriptor of a class's method or field. */
data class Desc(val owner: String, val name: String, val descriptor: String)

/** The [org.squiddev.cobalt.Varargs] class. */
val VARARGS = Type.getObjectType("org/squiddev/cobalt/Varargs")

/** The [org.squiddev.cobalt.LuaState] class. */
val LUA_STATE = Type.getObjectType("org/squiddev/cobalt/LuaState")

/** The [org.squiddev.cobalt.LuaValue] class. */
val LUA_VALUE = Type.getObjectType("org/squiddev/cobalt/LuaValue")

/** The [org.squiddev.cobalt.LuaError] class. */
val LUA_ERROR = Type.getObjectType("org/squiddev/cobalt/LuaError")

/** The [org.squiddev.cobalt.UnwindThrowable] throwable. */
val UNWIND_THROWABLE = Type.getObjectType("org/squiddev/cobalt/UnwindThrowable")

/** The [org.squiddev.cobalt.unwind.AutoUnwind] annotation. */
val AUTO_UNWIND = Type.getObjectType("org/squiddev/cobalt/unwind/AutoUnwind")

/** The [org.squiddev.cobalt.unwind.Pause] throwable. */
val PAUSE = Type.getObjectType("org/squiddev/cobalt/unwind/Pause")

/** The [org.squiddev.cobalt.unwind.UnwindState] class. */
val UNWIND_STATE = Type.getObjectType("org/squiddev/cobalt/unwind/UnwindState")

/** The [org.squiddev.cobalt.unwind.SuspendedTask] class. */
val SUSPENDED_TASK = Type.getObjectType("org/squiddev/cobalt/unwind/SuspendedTask")

/** The [org.squiddev.cobalt.unwind.SuspendedTask.Action] class. */
val SUSPENDED_TASK_ACTION = Type.getObjectType("org/squiddev/cobalt/unwind/SuspendedTask\$Action")

/** The [org.squiddev.cobalt.unwind.SuspendedFunction] class. */
val SUSPENDED_FUNCTION = Type.getObjectType("org/squiddev/cobalt/unwind/SuspendedFunction")

/** The lambda metafactory methodhandle */
val LAMBDA_METAFACTORY = Handle(
	Opcodes.H_INVOKESTATIC,
	"java/lang/invoke/LambdaMetafactory",
	"metafactory",
	"(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
	false,
)

/** The [org.squiddev.cobalt.debug.DebugFrame] class */
val DEBUG_FRAME = Type.getObjectType("org/squiddev/cobalt/debug/DebugFrame")
