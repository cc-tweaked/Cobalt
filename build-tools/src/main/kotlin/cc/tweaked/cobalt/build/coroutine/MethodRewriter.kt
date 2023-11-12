package cc.tweaked.cobalt.build.coroutine

import cc.tweaked.cobalt.build.*
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.commons.LocalVariablesSorter
import org.objectweb.asm.tree.*
import kotlin.math.ceil

private val OBJECT = Type.getObjectType("java/lang/Object")
private val ILLEGAL_STATE_EXCEPTION = Type.getObjectType("java/lang/IllegalStateException")

/** Add an [Object] argument to the end of a signature. */
fun rewriteDesc(desc: String): String = Type.getMethodDescriptor(
	Type.getReturnType(desc),
	*Type.getArgumentTypes(desc),
	OBJECT,
)

/** Find (and emit) the state class required for this function, based on the maximum number of values we need to save. */
private fun makeStateClass(generator: ClassEmitter, yieldPoints: List<YieldPoint>): Type {
	var longCounts = 0
	var objectCounts = 0

	for (yieldPoint in yieldPoints) {
		var thisLongCounts = 0
		var thisObjectCounts = 0

		for (local in yieldPoint.locals) {
			when (local) {
				is LocalValue.Skip -> continue
				is LocalValue.Saved -> if (local.ty.isReference) thisObjectCounts++ else thisLongCounts++
			}
		}

		longCounts = longCounts.coerceAtLeast(thisLongCounts)
		objectCounts = objectCounts.coerceAtLeast(thisObjectCounts)
	}

	if (longCounts == 0 && objectCounts == 0) return UNWIND_STATE

	// Clamp to the next "4", to avoid generating lots of different classes.
	longCounts = (ceil(longCounts / 4.0) * 4).toInt()
	objectCounts = (ceil(objectCounts / 4.0) * 4).toInt()

	val name = "${UNWIND_STATE.internalName}\$L${longCounts}O${objectCounts}"
	generator.generate(name, flags = ClassWriter.COMPUTE_FRAMES) { cw ->
		cw.visit(V1_8, ACC_PUBLIC.or(ACC_FINAL).or(ACC_SYNTHETIC), name, null, UNWIND_STATE.internalName, null)
		for (i in 0 until longCounts) cw.visitField(ACC_PUBLIC, "l$i", "J", null, null).visitEnd()
		for (i in 0 until objectCounts) cw.visitField(ACC_PUBLIC, "o$i", OBJECT.descriptor, null, null).visitEnd()

		val constructor = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)
		constructor.visitCode()
		constructor.visitVarInsn(ALOAD, 0)
		constructor.visitMethodInsn(INVOKESPECIAL, UNWIND_STATE.internalName, "<init>", "()V", false)
		constructor.visitInsn(RETURN)
		constructor.visitMaxs(1, 1)
		constructor.visitEnd()

		// Create a helper function in the style of UnwindState.getOrCreate. We call this function a lot, so it's worth
		// moving it into its own helper.
		cw.visitMethod(ACC_PUBLIC.or(ACC_STATIC), "getOrCreate", "(${OBJECT.descriptor})L$name;", null, null)
			.also { mw ->
				mw.visitCode()

				val isNotNull = Label()
				val endIf = Label()
				mw.visitVarInsn(ALOAD, 0)
				mw.visitJumpInsn(IFNONNULL, isNotNull)
				// if(null): new State();
				mw.visitTypeInsn(NEW, name)
				mw.visitInsn(DUP)
				mw.visitMethodInsn(INVOKESPECIAL, name, "<init>", "()V", false)
				mw.visitJumpInsn(GOTO, endIf)
				// if(non-null): (State) state
				mw.visitLabel(isNotNull)
				mw.visitVarInsn(ALOAD, 0)
				mw.visitTypeInsn(CHECKCAST, name)
				// end
				mw.visitLabel(endIf)
				mw.visitInsn(ARETURN)

				mw.visitMaxs(2, 1)
				mw.visitEnd()
			}

		cw.visitEnd()
	}

	return Type.getObjectType(name)
}

/** Get the exception thrown at a specific yield point. */
private fun getExceptionType(yieldType: YieldType): Type = when (yieldType) {
	is YieldType.AutoUnwind -> PAUSE
	is YieldType.Resume, is YieldType.Direct -> UNWIND_THROWABLE
	is YieldType.Unsupported -> throw IllegalStateException("YieldType.UNSUPPORTED should not be emitted here.")
}

/** Add labels after each yield point, so we can insert our try/catch handler. */
private fun instrumentMethodCalls(method: MethodNode, yieldPoints: List<YieldPoint>): List<Label> =
	yieldPoints.map { yieldPoint ->
		// TODO: This method (especially adding the null argument) is conceptually quite nasty. It'd be nice to mutate the
		//  method node, but it's not the end of the world.

		// @AutoUnwind calls require an implicit state argument. Note this must appear BEFORE the label.
		if (yieldPoint.yieldType == YieldType.AutoUnwind) method.instructions.insertBefore(yieldPoint.label, InsnNode(ACONST_NULL))

		// We also require a label after each resume point, so we can wrap it in a try block.
		when (val after = yieldPoint.yieldAt.next) {
			is LabelNode -> after.label
			else -> {
				val label = LabelNode()
				method.instructions.insert(yieldPoint.yieldAt, label)
				label.label
			}
		}
	}

/**
 * The main rewriter for @AutoUnwind-annotated functions.
 *
 * This is implemented as a simple [MethodVisitor], which hooks into [MethodVisitor.visitCode] and
 * [MethodVisitor.visitMaxs] to add hooks at the start and end of the function.
 */
private class AutoUnwindRewriter(
	access: Int, desc: String,
	private val yieldPoints: List<YieldPoint>,
	private val afterLabels: List<Label>,
	classEmitter: ClassEmitter,
	private val definitions: DefinitionData,
	private val sink: MethodVisitor,
) : MethodVisitor(ASM9) {
	private val resume = Label()
	private val rebuild = yieldPoints.map { Label() }
	private val tryCatches = yieldPoints.map { Label() }

	private val state = makeStateClass(classEmitter, yieldPoints)
	private val stateLocal: Int

	init {
		// Most instructions actually go through the LocalVariablesSorter, which correctly remaps locals to take account
		// of the state argument.
		val localVariables = LocalVariablesSorter(access, desc, sink)
		stateLocal = localVariables.newLocal(OBJECT)
		mv = localVariables
	}

	override fun visitCode() {
		super.visitCode()

		// try/catch blocks need to appear before their original label, so let's just to the easy thing and emit them
		// right off the ba.t
		for ((i, yieldPoint) in yieldPoints.withIndex()) {
			sink.visitTryCatchBlock(yieldPoint.label.label, afterLabels[i], tryCatches[i], getExceptionType(yieldPoint.yieldType).internalName)
		}

		// Generate if(state != null) goto resume
		sink.visitVarInsn(ALOAD, stateLocal)
		sink.visitJumpInsn(IFNONNULL, resume)
	}

	override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
		when (definitions.getYieldType(owner, name, descriptor)) {
			null, is YieldType.Resume, is YieldType.Direct -> super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
			is YieldType.AutoUnwind -> super.visitMethodInsn(opcode, owner, name, rewriteDesc(descriptor), isInterface)
			is YieldType.Unsupported -> throw IllegalStateException("YieldType.UNSUPPORTED should not be emitted here.")
		}
	}

	override fun visitMaxs(maxStack: Int, maxLocals: Int) {
		// First start off with our recovery code.
		sink.visitLabel(resume)

		// state = (State) state
		sink.visitVarInsn(ALOAD, stateLocal)
		sink.visitTypeInsn(CHECKCAST, state.internalName)
		sink.visitInsn(DUP)
		sink.visitVarInsn(ASTORE, stateLocal)

		// switch(state.state) {
		val invalidState = Label()
		sink.visitFieldInsn(GETFIELD, state.internalName, "state", "I")
		if (yieldPoints.size == 1) {
			// TABLESWITCH instructions are very large - in the trivial case we can do a much smaller comparison. We
			// could probably do an if-else chain for small sizes too, but then we have to load the state multiple times
			// so the trade-offs are less clear.
			sink.visitJumpInsn(IFEQ, rebuild[0])
		} else {
			sink.visitTableSwitchInsn(0, yieldPoints.size - 1, invalidState, *rebuild.toTypedArray())
		}

		// default: throw new IllegalStateExeception("Resuming into unknown state")
		sink.visitLabel(invalidState)
		sink.visitTypeInsn(NEW, ILLEGAL_STATE_EXCEPTION.internalName)
		sink.visitInsn(DUP)
		sink.visitLdcInsn("Resuming into unknown state")
		sink.visitMethodInsn(INVOKESPECIAL, ILLEGAL_STATE_EXCEPTION.internalName, "<init>", "(Ljava/lang/String;)V", false)
		sink.visitInsn(ATHROW)

		// Emit recovery code for each state.
		for ((i, yieldPoint) in yieldPoints.withIndex()) emitResume(i, yieldPoint)

		// And emit our try/catch handlers.
		for ((i, yieldPoint) in yieldPoints.withIndex()) emitSuspend(i, yieldPoint)

		// maxStack is easy to compute correctly, but not worth doing when we're computing frames already.
		super.visitMaxs(0, maxLocals)
	}

	/** Emit the code to resume at a specific yield point. */
	private fun emitResume(stateIdx: Int, yieldPoint: YieldPoint) {
		sink.visitLabel(rebuild[stateIdx])

		// Store the locals needed to our state.
		var longIdx = 0
		var objectIdx = 0
		for ((localIdx, local) in yieldPoint.locals.withIndex()) {
			when (local) {
				is LocalValue.Skip -> continue

				is LocalValue.Saved -> {
					sink.visitVarInsn(ALOAD, stateLocal)
					if (local.ty.isReference) {
						sink.visitFieldInsn(GETFIELD, state.internalName, "o$objectIdx", OBJECT.descriptor)
						sink.visitTypeInsn(CHECKCAST, local.ty.internalName)
						objectIdx++
					} else {
						sink.visitFieldInsn(GETFIELD, state.internalName, "l$longIdx", "J")
						unpackValue(local.ty)
						longIdx++
					}

					super.visitVarInsn(local.ty.getOpcode(ISTORE), localIdx) // Use super to remap the local
				}
			}
		}

		// Recover the initial stack
		for (stackVal in yieldPoint.stack) recoverValue(stackVal)

		// And then actually recover the function call.
		when (val yieldType = yieldPoint.yieldType) {
			// Push a dummy value for each argument and then jump to the original call.
			is YieldType.AutoUnwind -> {
				for (stackTy in Type.getArgumentTypes(yieldPoint.yieldAt.desc)) sink.visitInsn(stackTy.getDefaultOpcode())
				sink.visitVarInsn(ALOAD, stateLocal)
				sink.visitFieldInsn(GETFIELD, UNWIND_STATE.internalName, "child", OBJECT.descriptor)
				sink.visitJumpInsn(GOTO, yieldPoint.label.label)
			}

			// Take the varargs out of the state, and then call the resume(Varargs) method, wrapping it in a try/catch.
			is YieldType.Resume -> {
				takeResumeArgs()

				val before = Label()
				val after = Label()
				sink.visitTryCatchBlock(before, after, tryCatches[stateIdx], UNWIND_THROWABLE.internalName)
				sink.visitLabel(before)
				sink.visitMethodInsn(
					yieldPoint.yieldAt.opcode,
					yieldPoint.yieldAt.owner,
					"resume",
					Type.getMethodDescriptor(Type.getReturnType(yieldPoint.yieldAt.desc), VARARGS),
					yieldPoint.yieldAt.itf,
				)
				sink.visitLabel(after)
				sink.visitJumpInsn(GOTO, afterLabels[stateIdx])
			}

			// Take the varargs out of the state and then apply our projection function.
			is YieldType.Direct -> {
				takeResumeArgs()
				yieldType.extract(sink)
				sink.visitJumpInsn(GOTO, afterLabels[stateIdx])
			}

			is YieldType.Unsupported -> throw IllegalStateException("YieldType.UNSUPPORTED should not be emitted here.")
		}
	}

	/**
	 * Emit the code to suspend at a specific yield point.
	 *
	 * This creates a state object (if needed) and then dumps the required locals to it. We keep the exception on the
	 * stack at all points, to save having to allocate a local for it.
	 */
	private fun emitSuspend(stateIdx: Int, yieldPoint: YieldPoint) {
		// try { f() } catch (Pause _) { ... }
		val tryCatch = tryCatches[stateIdx]
		sink.visitLabel(tryCatch)

		// state = State.getOrCreate(state);
		sink.visitVarInsn(ALOAD, stateLocal)
		sink.visitMethodInsn(INVOKESTATIC, state.internalName, "getOrCreate", "(${OBJECT.descriptor})${state.descriptor}", false)
		sink.visitVarInsn(ASTORE, stateLocal)

		when (yieldPoint.yieldType) {
			is YieldType.AutoUnwind -> {
				// e.pushState(child)
				sink.visitInsn(DUP)
				sink.visitVarInsn(ALOAD, stateLocal)
				sink.visitMethodInsn(INVOKEVIRTUAL, PAUSE.internalName, "pushState", "(${UNWIND_STATE.descriptor})V", false)
			}

			is YieldType.Resume, is YieldType.Direct -> {
				sink.visitTypeInsn(NEW, PAUSE.internalName)
				sink.visitInsn(DUP_X1) // Stack is [new Pause(), (e : UnwindThrowable), new Pause()]
				sink.visitInsn(SWAP) // Stack is [new Pause(), new Pause(), (e : UnwindThrowable)]
				sink.visitVarInsn(ALOAD, stateLocal)
				sink.visitMethodInsn(INVOKESPECIAL, PAUSE.internalName, "<init>", "(${UNWIND_THROWABLE.descriptor}${UNWIND_STATE.descriptor})V", false)
			}

			is YieldType.Unsupported -> throw IllegalStateException("YieldType.UNSUPPORTED should not be emitted here.")
		}

		// state.state = stateIdx
		sink.visitVarInsn(ALOAD, stateLocal)
		sink.visitLoadInt(stateIdx)
		sink.visitFieldInsn(PUTFIELD, UNWIND_STATE.internalName, "state", "I")

		var longIdx = 0
		var objectIdx = 0
		for ((localIdx, local) in yieldPoint.locals.withIndex()) {
			when (local) {
				is LocalValue.Skip -> continue
				is LocalValue.Saved -> {
					sink.visitVarInsn(ALOAD, stateLocal)
					super.visitVarInsn(local.ty.getOpcode(ILOAD), localIdx) // Use super to remap the local

					if (local.ty.isReference) {
						sink.visitFieldInsn(PUTFIELD, state.internalName, "o$objectIdx", OBJECT.descriptor)
						objectIdx++
					} else {
						packValue(local.ty)
						sink.visitFieldInsn(PUTFIELD, state.internalName, "l$longIdx", "J")
						longIdx++
					}
				}
			}
		}

		// throw exception
		sink.visitInsn(ATHROW)
	}

	private fun takeResumeArgs() {
		// Pull the varargs from the current state
		sink.visitVarInsn(ALOAD, stateLocal)
		sink.visitFieldInsn(GETFIELD, UNWIND_STATE.internalName, "resumeArgs", VARARGS.descriptor)

		// And then clear it.
		sink.visitVarInsn(ALOAD, stateLocal)
		sink.visitInsn(ACONST_NULL)
		sink.visitFieldInsn(PUTFIELD, UNWIND_STATE.internalName, "resumeArgs", VARARGS.descriptor)
	}

	/** Pack a primitive value into a long. */
	private fun packValue(type: Type): Unit = when (type.sort) {
		Type.LONG -> Unit
		// We widen an int to a long
		Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> sink.visitInsn(I2L)
		// Floats and doubles are packed into longs.
		Type.DOUBLE -> sink.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "doubleToRawLongBits", "(D)J", false)
		Type.FLOAT -> {
			sink.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "floatToRawIntBits", "(F)I", false)
			sink.visitInsn(I2L)
		}

		else -> throw IllegalArgumentException("Cannot pack non-primitive $type")
	}

	/** Unpack a primitive value from a long. */
	private fun unpackValue(type: Type): Unit = when (type.sort) {
		Type.LONG -> Unit
		Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> sink.visitInsn(L2I)
		Type.DOUBLE -> sink.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "longBitsToDouble", "(J)D", false)
		Type.FLOAT -> {
			sink.visitInsn(L2I)
			sink.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "longBitsToFloat", "(I)F", false)
		}

		else -> throw IllegalArgumentException("Cannot unpack non-primitive $type")
	}

	private fun recoverValue(stackVal: StackValue): Unit = when (stackVal) {
		is StackValue.Constant -> stackVal.insn.accept(sink)
		is StackValue.Local -> stackVal.insn.accept(this) // Use this to remap locals.
		is StackValue.Field -> {
			recoverValue(stackVal.ownerValue)
			sink.visitFieldInsn(GETFIELD, stackVal.owner, stackVal.name, stackVal.desc)
		}
	}
}

/**
 * Rewrite the given [AUTO_UNWIND] annotated method, instrumenting it with the appropriate code for suspending and
 * resuming.
 */
fun instrumentAutoUnwind(
	method: MethodNode,
	yieldPoints: List<YieldPoint>,
	emitter: ClassEmitter,
	definitions: DefinitionData,
	mw: MethodVisitor,
) {
	val labels = instrumentMethodCalls(method, yieldPoints)
	method.accept(AutoUnwindRewriter(method.access, method.desc, yieldPoints, labels, emitter, definitions, mw))
}

private fun protect(childName: Type, method: MethodVisitor, body: () -> Unit) {
	val before = Label()
	val after = Label()
	val recover = Label()

	method.visitTryCatchBlock(before, after, recover, PAUSE.internalName)
	method.visitLabel(before)
	body()
	method.visitLabel(after)
	method.visitInsn(ARETURN)

	method.visitLabel(recover)
	method.visitFrame(F_FULL, 1, arrayOf(childName.internalName), 1, arrayOf(PAUSE.internalName))

	// this.state = pause.state
	method.visitInsn(DUP)
	method.visitFieldInsn(GETFIELD, PAUSE.internalName, "state", OBJECT.descriptor)
	method.visitVarInsn(ALOAD, 0)
	method.visitInsn(SWAP)
	method.visitFieldInsn(PUTFIELD, childName.internalName, "state", OBJECT.descriptor)

	// this.resumeAt = pause.resumeAt
	method.visitInsn(DUP)
	method.visitFieldInsn(GETFIELD, PAUSE.internalName, "resumeAt", UNWIND_STATE.descriptor)
	method.visitVarInsn(ALOAD, 0)
	method.visitInsn(SWAP)
	method.visitFieldInsn(PUTFIELD, childName.internalName, "resumeAt", UNWIND_STATE.descriptor)

	// throw e.getCause();
	method.visitMethodInsn(INVOKEVIRTUAL, PAUSE.internalName, "getCause", Type.getMethodDescriptor(UNWIND_THROWABLE), false)
	method.visitInsn(ATHROW)
}

/**
 * Generate a [SUSPENDED_FUNCTION] for a given method reference.
 *
 * This generates a constructor (and static factory function) which consumes the function's arguments and stores them
 * as fields. This is then passed to the original function when the [SUSPENDED_FUNCTION] is called.
 */
private fun makeSuspendedFunction(emitter: ClassEmitter, methodReference: Handle): Pair<Type, String> {
	val childName = Type.getObjectType("${methodReference.owner}\$${methodReference.name}")
	val originalArgTypes = Type.getArgumentTypes(methodReference.desc)
	val (invokeOpcode, argTypes) = when (methodReference.tag) {
		H_INVOKESTATIC -> Pair(INVOKESTATIC, originalArgTypes)
		H_INVOKEVIRTUAL -> Pair(INVOKEVIRTUAL, arrayOf(Type.getObjectType(methodReference.owner), *originalArgTypes))
		else -> throw UnsupportedConstruct("Cannot handle method refernece $methodReference")
	}
	val factoryDesc = Type.getMethodType(childName, *argTypes).descriptor

	emitter.generate(childName.internalName) { cw ->
		cw.visit(V1_8, ACC_FINAL, childName.internalName, null, OBJECT.internalName, arrayOf(SUSPENDED_FUNCTION.internalName))

		cw.visitField(ACC_PRIVATE, "state", OBJECT.descriptor, null, null).visitEnd()
		cw.visitField(ACC_PRIVATE, "resumeAt", UNWIND_STATE.descriptor, null, null).visitEnd()
		for ((i, arg) in argTypes.withIndex()) {
			cw.visitField(ACC_PRIVATE.or(ACC_FINAL), "arg$i", arg.descriptor, null, null).visitEnd()
		}

		// Create a constructor which saves each argument (for calling with run())
		val ctorDesc = Type.getMethodType(Type.VOID_TYPE, *argTypes).descriptor
		cw.visitMethod(ACC_PRIVATE, "<init>", ctorDesc, null, null).let { ctor ->
			ctor.visitCode()
			ctor.visitVarInsn(ALOAD, 0)
			ctor.visitMethodInsn(INVOKESPECIAL, OBJECT.internalName, "<init>", "()V", false)
			for ((i, arg) in argTypes.withIndex()) {
				ctor.visitVarInsn(ALOAD, 0)
				ctor.visitVarInsn(arg.getOpcode(ILOAD), i + 1)
				ctor.visitFieldInsn(PUTFIELD, childName.internalName, "arg$i", arg.descriptor)
			}
			ctor.visitInsn(RETURN)
			ctor.visitMaxs(2, argTypes.size + 1)
			ctor.visitEnd()
		}

		// Create a constructor which saves each argument (for calling with run())
		cw.visitMethod(ACC_STATIC, "make", factoryDesc, null, null).let { factory ->
			factory.visitCode()
			factory.visitTypeInsn(NEW, childName.internalName)
			factory.visitInsn(DUP)
			for ((i, arg) in argTypes.withIndex()) factory.visitVarInsn(arg.getOpcode(ILOAD), i)
			factory.visitMethodInsn(INVOKESPECIAL, childName.internalName, "<init>", ctorDesc, false)
			factory.visitInsn(ARETURN)
			factory.visitMaxs(argTypes.size + 2, argTypes.size)
			factory.visitEnd()
		}

		// Call method loads each value and then executes it.
		cw.visitMethod(
			ACC_PUBLIC, "call", Type.getMethodDescriptor(OBJECT, LUA_STATE), null,
			arrayOf(UNWIND_THROWABLE.internalName, LUA_ERROR.internalName),
		).let { call ->
			call.visitCode()

			// try { return f(..., null) } catch { ... }
			for ((i, arg) in argTypes.withIndex()) {
				call.visitVarInsn(ALOAD, 0)
				call.visitFieldInsn(GETFIELD, childName.internalName, "arg$i", arg.descriptor)
			}
			call.visitInsn(ACONST_NULL)
			protect(childName, call) {
				call.visitMethodInsn(invokeOpcode, methodReference.owner, methodReference.name, rewriteDesc(methodReference.desc), false)
			}

			call.visitMaxs((argTypes.size + 1).coerceAtLeast(3), 2)
			call.visitEnd()
		}

		// Call method loads each value and then executes it.
		cw.visitMethod(
			ACC_PUBLIC, "resume", Type.getMethodDescriptor(OBJECT, VARARGS), null,
			arrayOf(UNWIND_THROWABLE.internalName, LUA_ERROR.internalName),
		).let { resume ->
			resume.visitCode()

			// this.resumeAt.resumeArgs = args
			resume.visitVarInsn(ALOAD, 0)
			resume.visitFieldInsn(GETFIELD, childName.internalName, "resumeAt", UNWIND_STATE.descriptor)
			resume.visitVarInsn(ALOAD, 1)
			resume.visitFieldInsn(PUTFIELD, UNWIND_STATE.internalName, "resumeArgs", VARARGS.descriptor)

			// try { return f(..., this.state) } catch { ... }
			for ((i, arg) in argTypes.withIndex()) {
				resume.visitVarInsn(ALOAD, 0)
				resume.visitFieldInsn(GETFIELD, childName.internalName, "arg$i", arg.descriptor)
			}
			resume.visitVarInsn(ALOAD, 0)
			resume.visitFieldInsn(GETFIELD, childName.internalName, "state", OBJECT.descriptor)
			protect(childName, resume) {
				resume.visitMethodInsn(invokeOpcode, methodReference.owner, methodReference.name, rewriteDesc(methodReference.desc), false)
			}

			resume.visitMaxs((argTypes.size + 1).coerceAtLeast(3), 2)
			resume.visitEnd()
		}
	}

	return Pair(childName, factoryDesc)
}

/**
 * Rewrite methods which call static functions on [SUSPENDED_ACTION].
 */
fun instrumentDispatch(method: MethodNode, emitter: ClassEmitter, mw: MethodVisitor) {
	// We do our modifications directly on the MethodNode - it's a bit sad, but much easier as we're dealing with
	// two adjacent nodes.
	for (insn in method.instructions) {
		if (insn.opcode != INVOKESTATIC || (insn as MethodInsnNode).owner != SUSPENDED_ACTION.internalName) continue

		val invokeInsn = insn.previous as InvokeDynamicInsnNode
		val methodReference = invokeInsn.bsmArgs[1] as Handle
		when (insn.name) {
			"noYield" -> {
				// We replace the invokeDynamic and noYield call with a try { f() } catch(Pause e) { throw AssertionError() } clase
				val before = LabelNode()
				val after = LabelNode()

				// Replace the INVOKEDYNAMIC and INVOKESTATIc with a direct call to the function.
				val insertInsns = InsnList()
				insertInsns.add(before)
				insertInsns.add(InsnNode(ACONST_NULL))
				insertInsns.add(MethodInsnNode(INVOKESTATIC, methodReference.owner, methodReference.name, rewriteDesc(methodReference.desc), false))
				insertInsns.add(after)

				method.instructions.insert(insn, insertInsns)
				method.instructions.remove(invokeInsn)
				method.instructions.remove(insn)

				// Then emit a try/catch block which raises an exception if this function yields.
				val tryCatch = LabelNode()
				method.tryCatchBlocks.add(TryCatchBlockNode(before, after, tryCatch, PAUSE.internalName))
				method.instructions.add(tryCatch)
				method.instructions.add(FrameNode(F_NEW, 0, emptyArray(), 1, arrayOf(PAUSE.internalName)))
				method.instructions.add(TypeInsnNode(NEW, ILLEGAL_STATE_EXCEPTION.internalName))
				method.instructions.add(InsnNode(DUP))
				method.instructions.add(LdcInsnNode("This method should not have yielded."))
				method.instructions.add(MethodInsnNode(INVOKESPECIAL, ILLEGAL_STATE_EXCEPTION.internalName, "<init>", "(Ljava/lang/String;)V", false))
				method.instructions.add(InsnNode(ATHROW))
			}

			"toFunction" -> {
				val (childName, factoryDesc) = makeSuspendedFunction(emitter, methodReference)

				method.instructions.insert(insn, MethodInsnNode(INVOKESTATIC, childName.internalName, "make", factoryDesc, false))
				method.instructions.remove(invokeInsn)
				method.instructions.remove(insn)
			}

			"run" -> {
				// We create a SuspendedFunction like above and then call run on it. We could probably make this more
				// efficient by only constructing the object if we yield, but I don't think it's worth it.
				val (childName, factoryDesc) = makeSuspendedFunction(emitter, methodReference)

				val toInsert = InsnList()
				toInsert.add(MethodInsnNode(INVOKESTATIC, childName.internalName, "make", factoryDesc, false))
				// At this point we've got the DebugFrame and SuspendedFunction on the stack. DUP_X1 to turn that into
				// [SuspendedFunction, DebugFrame, SuspendedFunction], then write our field.
				toInsert.add(InsnNode(DUP_X1))
				toInsert.add(FieldInsnNode(PUTFIELD, DEBUG_FRAME.internalName, "state", OBJECT.descriptor))
				// Then invoke the original method. We pass null as the LuaState, as it's never actually used.
				toInsert.add(InsnNode(ACONST_NULL))
				toInsert.add(MethodInsnNode(INVOKEVIRTUAL, childName.internalName, "call", Type.getMethodDescriptor(OBJECT, LUA_STATE)))
				toInsert.add(TypeInsnNode(CHECKCAST, VARARGS.internalName))

				method.instructions.insert(insn, toInsert)
				method.instructions.remove(invokeInsn)
				method.instructions.remove(insn)
			}

			else -> throw IllegalStateException("Unhandled call to SuspendedTask.${insn.name}")
		}
	}

	method.accept(mw)
}
