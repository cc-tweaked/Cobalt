package cc.tweaked.cobalt.build.coroutine

import cc.tweaked.cobalt.build.UnsupportedConstruct
import cc.tweaked.cobalt.build.logger
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.slf4j.Logger

private val logger: Logger = logger {}

sealed interface YieldType {

	/**
	 * This method is annotated with the [cc.tweaked.cobalt.build.coroutine.AUTO_UNWIND] annotation.
	 *
	 * Calls to this method (and the definition itself) will be instrumented to include an extra state parameter.
	 */
	object AutoUnwind : YieldType

	/**
	 * This method follows the resumable contract: when resuming we will invoke a separate "resume" method with the
	 * packed Varargs.
	 */
	object Resume : YieldType

	/**
	 * This value does not run again, instead applying a projection function to the varargs.
	 */
	class Direct(val extract: (MethodVisitor) -> Unit) : YieldType

	/**
	 * This method unwinds, but in a way not understood by our tooling.
	 */
	// TODO: Remove this and handle this specially elsewhere?
	object Unsupported : YieldType
}

enum class InstrumentType {
	/**
	 * This method is annotated with the [cc.tweaked.cobalt.build.coroutine.AUTO_UNWIND], and so should be instrumented
	 * as such.
	 */
	AUTO_UNWIND,

	/**
	 * This method calls one of the methods in `SuspendedTask`.
	 */
	DISPATCH_UNWIND,
}

/** Information about definitions on the classpath. */
interface DefinitionData {
	/** Check if a method will yield or otherwise unwind the stack. */
	fun getYieldType(owner: String, name: String, desc: String): YieldType?

	/** Check if this method should be instrumented. */
	fun getInstrumentType(owner: String, name: String, desc: String): InstrumentType?

	/** Check if a class's field is final. */
	fun isFieldFinal(owner: String, name: String, desc: String): Boolean
}

private val builtinMethods = run {
	val opHelper = "org/squiddev/cobalt/OperationHelper"
	val dispatch = "org/squiddev/cobalt/function/Dispatch"
	val nop = YieldType.Direct { }
	val first = YieldType.Direct { mw ->
		mw.visitMethodInsn(INVOKEVIRTUAL, VARARGS.internalName, "first", "()${LUA_VALUE.descriptor}", false)
	}
	val firstBool = YieldType.Direct { mw ->
		mw.visitMethodInsn(INVOKEVIRTUAL, VARARGS.internalName, "first", "()${LUA_VALUE.descriptor}", false)
		mw.visitMethodInsn(INVOKEVIRTUAL, LUA_VALUE.internalName, "toBoolean", "()Z", false)
	}
	val drop = YieldType.Direct { mw -> mw.visitInsn(POP) }

	mapOf(
		Desc("org/squiddev/cobalt/compiler/InputReader", "read", "()I") to YieldType.Resume,
		Desc("org/squiddev/cobalt/unwind/SuspendedFunction", "call", "(Lorg/squiddev/cobalt/LuaState;)Ljava/lang/Object;") to YieldType.Resume,
		Desc(dispatch, "call", Type.getMethodDescriptor(LUA_VALUE, LUA_STATE, LUA_VALUE)) to first,
		Desc(dispatch, "call", Type.getMethodDescriptor(LUA_VALUE, LUA_STATE, LUA_VALUE, LUA_VALUE)) to first,
		Desc(dispatch, "call", Type.getMethodDescriptor(LUA_VALUE, LUA_STATE, LUA_VALUE, LUA_VALUE, LUA_VALUE)) to first,
		Desc(dispatch, "invoke", Type.getMethodDescriptor(VARARGS, LUA_STATE, LUA_VALUE, VARARGS)) to nop,
		Desc(opHelper, "eq", Type.getMethodDescriptor(Type.BOOLEAN_TYPE, LUA_STATE, LUA_VALUE, LUA_VALUE)) to firstBool,
		Desc(opHelper, "lt", Type.getMethodDescriptor(Type.BOOLEAN_TYPE, LUA_STATE, LUA_VALUE, LUA_VALUE)) to firstBool,
		Desc(opHelper, "length", Type.getMethodDescriptor(LUA_VALUE, LUA_STATE, LUA_VALUE)) to first,
		Desc(opHelper, "toString", Type.getMethodDescriptor(LUA_VALUE, LUA_STATE, LUA_VALUE)) to first,
		Desc(opHelper, "getTable", Type.getMethodDescriptor(LUA_VALUE, LUA_STATE, LUA_VALUE, Type.INT_TYPE)) to first,
		Desc(opHelper, "getTable", Type.getMethodDescriptor(LUA_VALUE, LUA_STATE, LUA_VALUE, LUA_VALUE)) to first,
		Desc(opHelper, "setTable", Type.getMethodDescriptor(Type.VOID_TYPE, LUA_STATE, LUA_VALUE, Type.INT_TYPE, LUA_VALUE)) to drop,
		Desc(opHelper, "setTable", Type.getMethodDescriptor(Type.VOID_TYPE, LUA_STATE, LUA_VALUE, LUA_VALUE, LUA_VALUE)) to drop,
	)
}

/**
 * Extracts useful information about the input classes, exposing it to other functions via the [DefinitionData] interface.
 */
class DefinitionScanner : DefinitionData {
	private val yieldingMethods = HashMap<Desc, YieldType>(builtinMethods)
	private val instrumentMethods = HashMap<Desc, InstrumentType>()
	private val finalFields = HashSet<Desc>()

	override fun getYieldType(owner: String, name: String, desc: String): YieldType? = yieldingMethods[Desc(owner, name, desc)]
	override fun getInstrumentType(owner: String, name: String, desc: String): InstrumentType? = instrumentMethods[Desc(owner, name, desc)]
	override fun isFieldFinal(owner: String, name: String, desc: String): Boolean = finalFields.contains(Desc(owner, name, desc))

	private inner class ClassScanner : ClassVisitor(Opcodes.ASM9) {
		private lateinit var className: String
		private var autoUnwind: Boolean = false
		var instrument: Boolean = false

		override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
			className = name
			super.visit(version, access, name, signature, superName, interfaces)
		}

		override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
			if (descriptor == AUTO_UNWIND.descriptor) autoUnwind = true
			return null
		}

		override fun visitField(access: Int, name: String, descriptor: String, signature: String?, value: Any?): FieldVisitor? {
			if ((access and Opcodes.ACC_FINAL) != 0) finalFields.add(Desc(className, name, descriptor))
			return null
		}

		override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor {
			val willUnwind = exceptions != null && exceptions.contains(UNWIND_THROWABLE.internalName)
			return object : MethodVisitor(Opcodes.ASM9) {
				// We create a method visitor which looks for calls to static methods on SuspendedTask. There's some
				// nastiness here as we want to keep track of the lambda which occurs just before that static call (as
				// that should also be treated as an @AutoUnwind function).

				var autoUnwindMethod: Boolean = false
				var lastLambda: Handle? = null

				fun clearLambda() {
					lastLambda = null
				}

				override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
					if (descriptor == AUTO_UNWIND.descriptor) autoUnwindMethod = true
					return null
				}

				// Discard the lambda we're currently tracking.
				override fun visitVarInsn(opcode: Int, varIndex: Int) = clearLambda()
				override fun visitInsn(opcode: Int) = clearLambda()
				override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) = clearLambda()
				override fun visitIntInsn(opcode: Int, operand: Int) = clearLambda()
				override fun visitTypeInsn(opcode: Int, type: String) = clearLambda()
				override fun visitJumpInsn(opcode: Int, label: Label) = clearLambda()
				override fun visitLdcInsn(value: Any) = clearLambda()
				override fun visitIincInsn(varIndex: Int, increment: Int) = clearLambda()
				override fun visitTableSwitchInsn(min: Int, max: Int, dflt: Label, vararg labels: Label) = clearLambda()
				override fun visitLookupSwitchInsn(dflt: Label?, keys: IntArray, labels: Array<out Label>) = clearLambda()
				override fun visitMultiANewArrayInsn(descriptor: String?, numDimensions: Int) = clearLambda()

				override fun visitInvokeDynamicInsn(name: String, descriptor: String, bootstrapMethodHandle: Handle, vararg bootstrapMethodArguments: Any) {
					clearLambda()

					// If we're invoking the lambda metafactory to make an Action, then keep track of this lambda.
					if (name == "run" && Type.getReturnType(descriptor) == SUSPENDED_ACTION && bootstrapMethodHandle == LAMBDA_METAFACTORY) {
						lastLambda = bootstrapMethodArguments[1] as Handle
					}
				}

				override fun visitMethodInsn(opcode: Int, owner: String, callName: String, callDesc: String, isInterface: Boolean) {
					// If we're invoking a static method on SuspendedTask, then both this function and the lambda should
					// be instrumented.
					if (opcode == INVOKESTATIC && owner == SUSPENDED_ACTION.internalName) {
						val lastLambda = this.lastLambda
						if (lastLambda == null || !lastLambda.name.contains("lambda")) {
							throw UnsupportedConstruct("$className.$name calls $owner.$callName with a non-lambda argument.")
						}

						yieldingMethods[Desc(lastLambda.owner, lastLambda.name, lastLambda.desc)] = YieldType.AutoUnwind
						instrumentMethods[Desc(lastLambda.owner, lastLambda.name, lastLambda.desc)] = InstrumentType.AUTO_UNWIND
						instrumentMethods[Desc(className, name, descriptor)] = InstrumentType.DISPATCH_UNWIND
						instrument = true
					}

					clearLambda()
				}

				override fun visitEnd() {
					when {
						// If we're annotated with @AutoUnwind and we will unwind, mark us as such.
						(autoUnwind || autoUnwindMethod) && willUnwind -> {
							yieldingMethods[Desc(className, name, descriptor)] = YieldType.AutoUnwind
							instrumentMethods[Desc(className, name, descriptor)] = InstrumentType.AUTO_UNWIND
							instrument = true
						}

						// If we're annotated with @AutoUnwind, but we don't unwind then something is very odd!
						autoUnwindMethod -> throw UnsupportedConstruct("$className.$name is annotated @AutoUnwind, but does not unwind.")

						// If this function unwinds, but is not @AutoUnwind function, then mark as a yielding call, but
						// one which is ignored by our transform. We use putIfAbsent here, as there are some hard-coded
						// methods which we want to handle specially.
						willUnwind -> {
							yieldingMethods.putIfAbsent(Desc(className, name, descriptor), YieldType.Unsupported)
						}
					}
				}
			}
		}
	}

	/**
	 * Add this class to our definition store, returning true iff it should be transformed further.
	 */
	fun addClass(classReader: ClassReader): Boolean {
		logger.debug("Loading {}", classReader.className)
		val scanner = ClassScanner()
		classReader.accept(scanner, 0)
		if (scanner.instrument) logger.info("Will instrument {}", classReader.className)
		return scanner.instrument
	}
}
