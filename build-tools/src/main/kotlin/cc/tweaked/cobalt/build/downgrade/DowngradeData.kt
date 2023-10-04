package cc.tweaked.cobalt.build.downgrade

import cc.tweaked.cobalt.build.coroutine.Desc
import cc.tweaked.cobalt.build.logger
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.commons.Remapper
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

private val logger = logger { }

/**
 * Gathers data about method calls in the program in order to find what private methods need to be widened.
 */
class DowngradeData {
	private class AccessInfo {
		var access: Int = 0
		var accessedExternally: Boolean = false
		var uniqueName: String? = null
	}

	private val members = HashMap<Desc, AccessInfo>()

	private fun getMember(owner: String, name: String, descriptor: String) =
		members.computeIfAbsent(Desc(owner, name, descriptor)) { AccessInfo() }

	private inner class ClassScanner : ClassVisitor(ASM9) {
		lateinit var className: String

		override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
			this.className = name
			super.visit(version, access, name, signature, superName, interfaces)
		}

		override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor {
			getMember(className, name, descriptor).access = access
			return object : MethodVisitor(ASM9) {
				override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
					if (owner != className) getMember(owner, name, descriptor).accessedExternally = true
					super.visitFieldInsn(opcode, owner, name, descriptor)
				}

				override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
					if (owner != className) getMember(owner, name, descriptor).accessedExternally = true
					super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
				}

				override fun visitInvokeDynamicInsn(name: String, descriptor: String, bootstrapMethodHandle: Handle, vararg bootstrapMethodArguments: Any) {
					for (arg in bootstrapMethodArguments) {
						if (arg is Handle) getMember(arg.owner, arg.name, arg.desc).accessedExternally = true
					}
					super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
				}
			}
		}

		override fun visitField(access: Int, name: String, descriptor: String, signature: String?, value: Any?): FieldVisitor? {
			getMember(className, name, descriptor).access = access
			return super.visitField(access, name, descriptor, signature, value)
		}
	}

	/**
	 * Determine if a method or field should be widened, removing its [ACC_PRIVATE] modifier.
	 */
	fun shouldWiden(owner: String, name: String, descriptor: String): Boolean {
		val info = members[Desc(owner, name, descriptor)] ?: return false
		return info.accessedExternally && info.access.and(ACC_PRIVATE) != 0
	}

	/**
	 * Get the name of a method.
	 *
	 * When accessing a private instance method, we rename it to a unique method, to prevent accidental overriding
	 * in child/parent classes.
	 *
	 * Typically one would use the [remapper] instead.
	 */
	fun getMethodName(owner: String, name: String, descriptor: String): String {
		val info = members[Desc(owner, name, descriptor)] ?: return name
		return if (info.accessedExternally && info.access.and(ACC_PRIVATE or ACC_STATIC) == ACC_PRIVATE && name != "<init>") {
			//  If this is a private instance member, which is accessed from another class, then we must rename it.
			info.uniqueName ?: run {
				val digest = MessageDigest.getInstance("MD5")
				digest.update(owner.toByteArray(StandardCharsets.UTF_8))
				digest.update(name.toByteArray(StandardCharsets.UTF_8))
				digest.update(descriptor.toByteArray(StandardCharsets.UTF_8))
				val hash = digest.digest()

				val uniqueName = String.format("%s$%02x%02x%02x", name, hash[0], hash[1], hash[2])
				logger.debug("Renaming private member {}.{}{} to {}", owner, name, descriptor, uniqueName)
				info.uniqueName = uniqueName
				uniqueName
			}
		} else name
	}

	val remapper = object : Remapper() {
		override fun mapMethodName(owner: String, name: String, desc: String): String = getMethodName(owner, name, desc)
	}

	fun addClass(classReader: ClassReader) {
		classReader.accept(ClassScanner(), 0)
	}
}
