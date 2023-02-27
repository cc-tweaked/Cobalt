package cc.squiddev.cobalt.build

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

/** A [ClassWriter] extension which avoids loading classes when computing frames. */
private class NonLoadingClassWriter(reader: ClassReader?, flags: Int) : ClassWriter(reader, flags) {
	override fun getCommonSuperClass(type1: String, type2: String): String {
		if (type1 == "java/lang/Object" || type2 == "java/lang/Object") return "java/lang/Object"

		logger.warn("Guessing the super-class of {} and {}.", type1, type2)
		return "java/lang/Object"
	}
}

