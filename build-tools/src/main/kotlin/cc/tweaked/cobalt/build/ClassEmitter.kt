package cc.tweaked.cobalt.build

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.util.CheckClassAdapter
import org.slf4j.Logger
import java.nio.file.Files
import java.nio.file.Path

private val logger: Logger = logger {}

/** Generate additional classes which don't exist in the original source set. */
interface ClassEmitter {
	/** Emit a class if it does not already exist. */
	fun generate(name: String, classReader: ClassReader? = null, flags: Int = 0, write: (ClassVisitor) -> Unit)
}

/** A basic implementation of [ClassEmitter] which stores the written classes in memory. */
class InMemoryClassEmitter : ClassEmitter {
	private val emitted: MutableMap<String, ByteArray> = HashMap()

	val classes: Map<String, ByteArray> = emitted

	override fun generate(name: String, classReader: ClassReader?, flags: Int, write: (ClassVisitor) -> Unit) {
		if (emitted.containsKey(name)) return

		logger.info("Writing {}", name)
		val cw = NonLoadingClassWriter(classReader, flags)
		write(CheckClassAdapter(cw))
		emitted[name] = cw.toByteArray()
	}
}

/** An implementation of [ClassEmitter] which writes files to a directory. */
class FileClassEmitter(private val outputDir: Path) : ClassEmitter {
	private val emitted = mutableSetOf<String>()
	override fun generate(name: String, classReader: ClassReader?, flags: Int, write: (ClassVisitor) -> Unit) {
		if (!emitted.add(name)) return

		logger.info("Writing {}", name)
		val cw = NonLoadingClassWriter(classReader, flags)
		write(CheckClassAdapter(cw))

		val outputFile = outputDir.resolve("$name.class")
		Files.createDirectories(outputFile.parent)
		Files.write(outputFile, cw.toByteArray())
	}
}

/** A unordered pair, such that (x, y) = (y, x) */
private class UnorderedPair<T>(private val x: T, private val y: T) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is UnorderedPair<*>) return false
		return (x == other.x && y == other.y) || (x == other.y && y == other.x)
	}

	override fun hashCode(): Int = x.hashCode() xor y.hashCode()
	override fun toString(): String = "UnorderedPair($x, $y)"
}

private val subclassRelations = mapOf(
	// TODO: Maybe we should infer this when visiting the original classes?
	UnorderedPair("org/squiddev/cobalt/LuaValue", "org/squiddev/cobalt/function/LuaFunction") to "org/squiddev/cobalt/LuaValue",
	UnorderedPair("org/squiddev/cobalt/LuaValue", "org/squiddev/cobalt/LuaInteger") to "org/squiddev/cobalt/LuaValue",
	UnorderedPair("org/squiddev/cobalt/LuaValue", "org/squiddev/cobalt/LuaBoolean") to "org/squiddev/cobalt/LuaValue",
	UnorderedPair("org/squiddev/cobalt/LuaValue", "org/squiddev/cobalt/LuaString") to "org/squiddev/cobalt/LuaValue",
	UnorderedPair("org/squiddev/cobalt/Varargs", "org/squiddev/cobalt/LuaValue") to "org/squiddev/cobalt/Varargs",
	UnorderedPair("org/squiddev/cobalt/LuaError", "org/squiddev/cobalt/compiler/CompileException") to "java/lang/Exception",
	UnorderedPair("org/squiddev/cobalt/LuaError", "java/lang/Exception") to "java/lang/Exception",
	UnorderedPair("org/squiddev/cobalt/compiler/CompileException", "java/lang/Exception") to "java/lang/Exception"
)

/** A [ClassWriter] extension which avoids loading classes when computing frames. */
private class NonLoadingClassWriter(reader: ClassReader?, flags: Int) : ClassWriter(reader, flags) {
	override fun getCommonSuperClass(type1: String, type2: String): String {
		if (type1 == "java/lang/Object" || type2 == "java/lang/Object") return "java/lang/Object"

		val subclass = subclassRelations[UnorderedPair(type1, type2)]
		if (subclass != null) return subclass

		logger.warn("Guessing the super-class of {} and {}.", type1, type2)
		return "java/lang/Object"
	}
}
