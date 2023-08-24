package cc.tweaked.cobalt.build

import cc.tweaked.cobalt.build.coroutine.CoroutineInstrumentation
import cc.tweaked.cobalt.build.coroutine.DefinitionScanner
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.system.exitProcess

class UnsupportedConstruct(message: String, cause: Exception? = null) : RuntimeException(message, cause)

fun main(args: Array<String>) {
	if (args.size != 2) {
		System.err.println("Expected: INPUT OUTPUT")
		exitProcess(1)
	}

	val inputDir = Paths.get(args[0])
	val outputDir = Paths.get(args[1])

	val definitions = DefinitionScanner()
	val instrumentedClasses = mutableListOf<ClassReader>()
	Files.find(inputDir, Int.MAX_VALUE, { path, _ -> path.extension == "class" }).use { files ->
		files.forEach { inputFile ->
			val reader = Files.newInputStream(inputFile).use { ClassReader(it) }
			if (definitions.addClass(reader)) {
				instrumentedClasses.add(reader)
			} else {
				val outputFile = outputDir.resolve(inputDir.relativize(inputFile))
				Files.createDirectories(outputFile.parent)
				Files.copy(inputFile, outputFile)
			}
		}
	}

	val emitter = FileClassEmitter(outputDir)
	for (klass in instrumentedClasses) {
		emitter.generate(klass.className, klass, ClassWriter.COMPUTE_FRAMES) { cw ->
			klass.accept(CoroutineInstrumentation(Opcodes.ASM9, cw, definitions, emitter), ClassReader.EXPAND_FRAMES)
		}
	}
}
