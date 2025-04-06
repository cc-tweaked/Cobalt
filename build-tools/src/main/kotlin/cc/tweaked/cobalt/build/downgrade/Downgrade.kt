@file:Suppress("BASE_CLASS_FIELD_SHADOWS_DERIVED_CLASS_PROPERTY", "NAME_SHADOWING")

package cc.tweaked.cobalt.build.downgrade

import cc.tweaked.cobalt.build.*
import cc.tweaked.cobalt.build.coroutine.SUSPENDED_TASK
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.commons.ClassRemapper

private val logger = logger { }

class Downgrade(
	private val emitter: ClassEmitter,
	private val data: DowngradeData,
	visitor: ClassVisitor,
) :
	ClassRemapper(ASM9, visitor, data.remapper) {
	private var uniqueIdx = 0

	internal val className: String
		get() = super.className

	override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<out String>?) {
		val (superName, signature) = if (superName == "java/lang/Record") {
			Pair("java/lang/Object", signature?.replace("Ljava/lang/Record;", "Ljava/lang/Object;"))
		} else Pair(superName, signature)

		super.visit(V1_6, access.and(ACC_RECORD.inv()), name, signature, superName, interfaces)
	}

	override fun visitNestMember(nestMember: String?) {}
	override fun visitNestHost(nestHost: String?) {}
	override fun visitPermittedSubclass(permittedSubclass: String?) {}
	override fun visitRecordComponent(name: String, descriptor: String, signature: String?): RecordComponentVisitor? =
		null

	override fun visitField(access: Int, name: String, descriptor: String, signature: String?, value: Any?): FieldVisitor {
		val access = when {
			data.shouldWiden(className, name, descriptor) -> access.and(ACC_PRIVATE.inv())
			else -> access
		}
		return super.visitField(access, name, descriptor, signature, value)
	}

	override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
		if (access.and(ACC_STATIC) != 0 && className == SUSPENDED_TASK.internalName) {
			logger.debug("Dropping static {}.{} method", className, name)
			return null
		}

		val access = when {
			data.shouldWiden(className, name, descriptor) -> access.and(ACC_PRIVATE.inv())
			else -> access
		}
		val mw = super.visitMethod(access, name, descriptor, signature, exceptions) ?: return null
		return DowngradeMethodVisitor(this, emitter, data, mw)
	}

	internal fun uniqueName(prefix: String): String {
		val name = "$prefix\$$uniqueIdx"
		uniqueIdx++
		return name
	}
}

private class DowngradeMethodVisitor(
	private val parent: Downgrade,
	private val emitter: ClassEmitter,
	private val data: DowngradeData,
	methodVisitor: MethodVisitor,
) : MethodVisitor(ASM9, methodVisitor) {
	override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
		if (opcode == INVOKESPECIAL && owner == "java/lang/Record" && name == "<init>" && descriptor == "()V") {
			super.visitMethodInsn(opcode, "java/lang/Object", name, descriptor, isInterface)
		} else {
			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
		}
	}

	override fun visitInvokeDynamicInsn(name: String, descriptor: String, handle: Handle, vararg arguments: Any) {
		if (handle.owner == "java/lang/invoke/LambdaMetafactory" && handle.name == "metafactory" && handle.desc == "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;") {
			visitLambda(name, descriptor, arguments[0] as Type, arguments[1] as Handle)
		} else if (handle.owner == "java/lang/invoke/StringConcatFactory" && handle.name == "makeConcatWithConstants" && handle.desc == "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;") {
			super.visitMethodInsn(INVOKESTATIC, parent.className, visitConcat(descriptor, arguments), descriptor, false)
		} else if (handle.owner == "java/lang/runtime/ObjectMethods" && handle.name == "bootstrap" && handle.desc == "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;") {
			when (name) {
				"equals" -> super.visitMethodInsn(INVOKESTATIC, parent.className, visitEquals(descriptor, arguments), descriptor, false)
				"hashCode" -> super.visitMethodInsn(INVOKESTATIC, parent.className, visitHashCode(descriptor, arguments), descriptor, false)
				"toString" -> super.visitMethodInsn(INVOKESTATIC, parent.className, visitToString(descriptor, arguments), descriptor, false)
				else -> super.visitInvokeDynamicInsn(name, descriptor, handle, *arguments)
			}
		} else {
			super.visitInvokeDynamicInsn(name, descriptor, handle, *arguments)
		}
	}

	private fun visitLambda(name: String, descriptor: String, signature: Type, lambda: Handle) {
		val interfaceTy = Type.getReturnType(descriptor)
		val fields = Type.getArgumentTypes(descriptor)

		val lambdaName = parent.uniqueName("lambda")
		val className = "${parent.className}\$$lambdaName"

		emitter.generate(className, flags = ClassWriter.COMPUTE_MAXS) { cw ->
			cw.visit(V1_6, ACC_FINAL, className, null, "java/lang/Object", arrayOf(interfaceTy.internalName))
			for ((i, ty) in fields.withIndex()) {
				cw.visitField(ACC_PRIVATE or ACC_FINAL, "field$i", ty.descriptor, null, null)
					.visitEnd()
			}

			cw.visitMethod(ACC_STATIC, "create", Type.getMethodDescriptor(interfaceTy, *fields), null, null).let { mw ->
				mw.visitCode()
				mw.visitTypeInsn(NEW, className)
				mw.visitInsn(DUP)
				for ((i, ty) in fields.withIndex()) mw.visitVarInsn(ty.getOpcode(ILOAD), i)
				mw.visitMethodInsn(INVOKESPECIAL, className, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, *fields), false)
				mw.visitInsn(ARETURN)
				mw.visitMaxs(0, 0)
				mw.visitEnd()
			}

			cw.visitMethod(0, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, *fields), null, null).let { mw ->
				mw.visitCode()
				mw.visitVarInsn(ALOAD, 0)
				mw.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
				for ((i, ty) in fields.withIndex()) {
					mw.visitVarInsn(ALOAD, 0)
					mw.visitVarInsn(ty.getOpcode(ILOAD), i + 1)
					mw.visitFieldInsn(PUTFIELD, className, "field$i", ty.descriptor)
				}
				mw.visitInsn(RETURN)
				mw.visitMaxs(0, 0)
				mw.visitEnd()
			}

			cw.visitMethod(ACC_PUBLIC, name, signature.descriptor, null, null).let { mw ->
				mw.visitCode()

				val targetArgs = when (lambda.tag) {
					H_INVOKEVIRTUAL, H_INVOKESPECIAL -> arrayOf(
						Type.getObjectType(lambda.owner),
						*Type.getArgumentTypes(lambda.desc),
					)

					H_INVOKESTATIC, H_NEWINVOKESPECIAL -> Type.getArgumentTypes(lambda.desc)
					else -> throw IllegalStateException("Unhandled opcode")
				}
				var targetArgOffset = 0

				// If we're a ::new method handle, create the object.
				if (lambda.tag == H_NEWINVOKESPECIAL) {
					mw.visitTypeInsn(NEW, lambda.owner)
					mw.visitInsn(DUP)
				}

				// Load our fields
				for ((i, ty) in fields.withIndex()) {
					mw.visitVarInsn(ALOAD, 0)
					mw.visitFieldInsn(GETFIELD, className, "field$i", ty.descriptor)

					val expectedTy = targetArgs[targetArgOffset]
					if (ty != expectedTy) println("$ty != $expectedTy")
					targetArgOffset++
				}

				// Load the additional arguments
				val arguments = signature.argumentTypes
				for ((i, ty) in arguments.withIndex()) {
					mw.visitVarInsn(ty.getOpcode(ILOAD), i + 1)
					val expectedTy = targetArgs[targetArgOffset]
					if (ty != expectedTy) {
						logger.debug("Argument {} for {} is {}, expected {}. adding a cast", i, interfaceTy, ty, expectedTy)
						mw.visitTypeInsn(CHECKCAST, expectedTy.internalName)
					}
					targetArgOffset++
				}

				// Invoke our init call
				mw.visitMethodInsn(
					when (lambda.tag) {
						H_INVOKEVIRTUAL, H_INVOKESPECIAL -> INVOKEVIRTUAL
						H_INVOKESTATIC -> INVOKESTATIC
						H_NEWINVOKESPECIAL -> INVOKESPECIAL
						else -> throw IllegalStateException("Unhandled opcode")
					},
					lambda.owner, data.getMethodName(lambda.owner, lambda.name, lambda.desc), lambda.desc, false,
				)

				if (lambda.tag != H_NEWINVOKESPECIAL) {
					val expectedRetTy = signature.returnType
					val retTy = Type.getReturnType(lambda.desc)
					when {
						// Types are the same, no-op
						expectedRetTy == retTy -> {}
						// If both are a reference, we assume the original code was well formed.
						retTy.isReference && expectedRetTy.isReference -> {}

						// Add some primitives.
						retTy == Type.INT_TYPE && expectedRetTy.descriptor == "Ljava/lang/Object;" -> {
							logger.debug("Return value for {} is {}, expected {}. adding a cast", interfaceTy, retTy, expectedRetTy)
							mw.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
						}

						else -> logger.error("Return value for {} is {}, expected {}. Not sure what to do!", interfaceTy, retTy, expectedRetTy)
					}
				}

				// A little ugly special handling for ::new
				mw.visitInsn(if (lambda.tag == H_NEWINVOKESPECIAL) ARETURN else signature.returnType.getOpcode(IRETURN))
				mw.visitMaxs(0, 0)
				mw.visitEnd()
			}

			cw.visitEnd()
		}

		visitMethodInsn(INVOKESTATIC, className, "create", Type.getMethodDescriptor(interfaceTy, *fields), false)
	}

	private fun visitConcat(descriptor: String, args: Array<out Any>): String {
		val name = parent.uniqueName("concat")
		val mw = parent.visitMethod(ACC_PRIVATE or ACC_STATIC, name, descriptor, null, null)!!
		mw.visitCode()

		val argTypes = Type.getArgumentTypes(descriptor)
		var stackIdx = 0
		var stackSlot = 0
		var constantIdx = 1
		val fragment = StringBuilder()

		mw.visitTypeInsn(NEW, STRING_BUILDER)
		mw.visitInsn(DUP)
		mw.visitMethodInsn(INVOKESPECIAL, STRING_BUILDER, "<init>", "()V", false)

		val template = args[0] as String
		for (c in template) {
			when (c) {
				'\u0001' -> {
					if (fragment.isNotEmpty()) {
						mw.visitLdcInsn(fragment.toString())
						mw.visitMethodInsn(
							INVOKEVIRTUAL,
							STRING_BUILDER, "append", "(Ljava/lang/String;)L$STRING_BUILDER;", false,
						)
						fragment.clear()
					}

					val ty = argTypes[stackIdx]
					mw.visitVarInsn(ty.getOpcode(ILOAD), stackSlot)

					val tyDescriptor = when {
						!ty.isReference -> ty.descriptor
						ty.internalName == "java/lang/String" -> "Ljava/lang/String;"
						else -> "Ljava/lang/Object;"
					}
					mw.visitMethodInsn(
						INVOKEVIRTUAL,
						STRING_BUILDER, "append", "($tyDescriptor)L$STRING_BUILDER;", false,
					)
					stackIdx++
					stackSlot += ty.size
				}

				'\u0002' -> {
					fragment.append(args[constantIdx])
					constantIdx++
				}

				else -> fragment.append(c)
			}
		}

		if (fragment.isNotEmpty()) {
			mw.visitLdcInsn(fragment.toString())
			mw.visitMethodInsn(
				INVOKEVIRTUAL,
				STRING_BUILDER, "append", "(Ljava/lang/String;)L$STRING_BUILDER;", false,
			)
			fragment.clear()
		}

		mw.visitMethodInsn(INVOKEVIRTUAL, STRING_BUILDER, "toString", "()Ljava/lang/String;", false)
		mw.visitInsn(ARETURN)

		mw.visitMaxs(1 + argTypes.maxOf { it.size }, argTypes.sumOf { it.size })
		mw.visitEnd()

		return name
	}

	private fun visitEquals(descriptor: String, args: Array<out Any>): String {
		val type = args[0] as Type
		val typeName = type.internalName

		val name = parent.uniqueName("equals")
		val mw = parent.visitMethod(ACC_PRIVATE or ACC_STATIC, name, descriptor, null, null)!!
		mw.visitCode()

		val fail = Label() // The "return false;" case.

		mw.visitVarInsn(ALOAD, 1)
		mw.visitTypeInsn(INSTANCEOF, typeName)
		mw.visitJumpInsn(IFEQ, fail)

		mw.visitVarInsn(ALOAD, 1)
		mw.visitTypeInsn(CHECKCAST, typeName)
		mw.visitVarInsn(ASTORE, 1)

		var size = 1
		for (i in 2 until args.size) {
			val field = args[i] as Handle
			assert(field.owner == typeName && field.tag == H_GETFIELD)

			mw.visitVarInsn(ALOAD, 0)
			mw.visitFieldInsn(GETFIELD, field.owner, field.name, field.desc)
			mw.visitVarInsn(ALOAD, 1)
			mw.visitFieldInsn(GETFIELD, field.owner, field.name, field.desc)

			val fieldType = Type.getType(field.desc)
			when (fieldType.sort) {
				Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> mw.visitJumpInsn(IF_ICMPNE, fail)
				Type.FLOAT -> {
					mw.visitInsn(FCMPL)
					mw.visitJumpInsn(IFNE, fail)
				}

				Type.DOUBLE -> {
					mw.visitInsn(DCMPL)
					mw.visitJumpInsn(IFNE, fail)
				}

				Type.LONG -> {
					mw.visitInsn(LCMP)
					mw.visitJumpInsn(IFNE, fail)
				}

				Type.OBJECT, Type.ARRAY -> {
					mw.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false)
					mw.visitJumpInsn(IFNE, fail)
				}

				else -> throw IllegalArgumentException("${fieldType.sort} is not a value type")
			}

			size = size.coerceAtLeast(2 * fieldType.size)
		}

		mw.visitInsn(ICONST_1)
		mw.visitInsn(IRETURN)
		mw.visitLabel(fail)
		mw.visitInsn(ICONST_0)
		mw.visitInsn(IRETURN)

		mw.visitMaxs(size, 2)
		mw.visitEnd()

		return name
	}

	private fun visitHashCode(descriptor: String, args: Array<out Any>): String {
		val type = args[0] as Type
		val typeName = type.internalName

		val name = parent.uniqueName("hashCode")
		val mw = parent.visitMethod(ACC_PRIVATE or ACC_STATIC, name, descriptor, null, null)!!
		mw.visitCode()

		var size = 1
		for (i in 2 until args.size) {
			val field = args[i] as Handle
			assert(field.owner == typeName && field.tag == H_GETFIELD)

			if (i > 2) {
				mw.visitLoadInt(31)
				mw.visitInsn(IMUL)
			}

			mw.visitVarInsn(ALOAD, 0)
			mw.visitFieldInsn(GETFIELD, field.owner, field.name, field.desc)

			val fieldType = Type.getType(field.desc)
			val primitiveType = fieldType.boxedName
			if (primitiveType == null) {
				mw.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "hashCode", "(Ljava/lang/Object;)I", false)
			} else {
				mw.visitMethodInsn(INVOKESTATIC, primitiveType, "hashCode", Type.getMethodDescriptor(Type.INT_TYPE, fieldType), false)
			}

			if (i > 2) {
				mw.visitInsn(IADD)
			}

			size = size.coerceAtLeast(1 + fieldType.size)
		}

		mw.visitInsn(IRETURN)

		mw.visitMaxs(size, 2)
		mw.visitEnd()

		return name
	}

	private fun visitToString(descriptor: String, args: Array<out Any>): String {
		val type = args[0] as Type
		val typeName = type.internalName

		val name = parent.uniqueName("toString")
		val mw = parent.visitMethod(ACC_PRIVATE or ACC_STATIC, name, descriptor, null, null)!!
		mw.visitCode()
		mw.visitLdcInsn("$typeName()")
		mw.visitInsn(ARETURN)
		mw.visitMaxs(1, 1)
		mw.visitEnd()

		return name
	}

	companion object {
		private const val STRING_BUILDER = "java/lang/StringBuilder"
	}
}
