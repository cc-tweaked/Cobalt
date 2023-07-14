package cc.tweaked.cobalt.build.coroutine

import cc.tweaked.cobalt.build.ClassEmitter
import cc.tweaked.cobalt.build.UnsupportedConstruct
import cc.tweaked.cobalt.build.logger
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode
import org.slf4j.Logger

private val logger: Logger = logger {}

/**
 * The main entrypoint for rewriting [AUTO_UNWIND]-related methods.
 */
class CoroutineInstrumentation(
	api: Int, out: ClassVisitor?,
	private val definitions: DefinitionData,
	private val emitter: ClassEmitter,
) : ClassVisitor(api, out) {
	private lateinit var className: String

	override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<out String>?) {
		className = name
		super.visit(version, access, name, signature, superName, interfaces)
	}

	override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
		// Accumulate this method into a MethodNode, and then emit it at the very end.
		return when (val def = definitions.getInstrumentType(className, name, descriptor)) {
			null -> super.visitMethod(access, name, descriptor, signature, exceptions)
			else -> {
				val node = MethodNode(access, name, descriptor, signature, exceptions)
				return object : MethodVisitor(api, node) {
					override fun visitEnd() {
						super.visitEnd()
						when (def) {
							InstrumentType.AUTO_UNWIND -> emitMethod(node, ::emitAutoUnwind)
							InstrumentType.DISPATCH_UNWIND -> emitMethod(node, ::emitDispatchUnwind)
						}
					}
				}
			}
		}
	}

	private inline fun emitMethod(method: MethodNode, fn: (node: MethodNode) -> Unit) {
		try {
			fn(method)
		} catch (e: UnsupportedConstruct) {
			throw UnsupportedConstruct("Failed inside $className.${method.name}", e)
		}
	}

	private fun emitAutoUnwind(method: MethodNode) {
		// Run our analysis passes.
		val blocks = extractBlocks(method.instructions, definitions)
		val resume = findYieldPoints(className, method, blocks, definitions)
		if (resume.isEmpty()) {
			val parent = super.visitMethod(method.access, method.name, method.desc, method.signature, method.exceptions?.toTypedArray())
			logger.error("Skipping {}.{} as it contains no resumption points.", className, method.name)
			method.accept(parent)
			return
		}

		// Replace UnwindThrowable with Pause - this isn't really important, but I guess is useful for documentation?
		val exceptions = method.exceptions.toTypedArray()
		exceptions[exceptions.indexOf(UNWIND_THROWABLE.internalName)] = PAUSE.internalName

		// If we're a lambda, make this symbol package private instead of private.
		val access = if (method.name.contains("lambda")) method.access and Opcodes.ACC_PRIVATE.inv() else method.access

		// And then re-emit the method.
		instrumentAutoUnwind(
			method, resume, emitter, definitions, super.visitMethod(access, method.name, rewriteDesc(method.desc), method.signature, exceptions),
		)
	}

	private fun emitDispatchUnwind(method: MethodNode) {
		instrumentDispatch(
			method, emitter, super.visitMethod(method.access, method.name, method.desc, method.signature, method.exceptions?.toTypedArray()),
		)
	}
}
