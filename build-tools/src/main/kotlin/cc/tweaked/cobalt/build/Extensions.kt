package cc.tweaked.cobalt.build

import org.objectweb.asm.ConstantDynamic
import org.objectweb.asm.Handle
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceMethodVisitor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter

/** Escape a string for including in a GraphViz dot file. */
internal fun String.escapeForGraphViz() = replace("\\", "\\\\").replace("\n", "\\l").replace("\"", "\\\"")

/** Escape and quote a string for including in a GraphViz dot file. */
internal fun String.quoteForGraphViz() = "\"" + escapeForGraphViz() + "\""

internal fun withMethodTraceVisitor(accept: (TraceMethodVisitor) -> Unit): String {
	val printer = Textifier()

	accept(TraceMethodVisitor(printer))

	val output = StringWriter()
	PrintWriter(output).use { printer.print(it) }
	return output.toString().trim()
}

/** Get the debug representation of a single instruction. This is incredibly inefficient, */
internal fun AbstractInsnNode.asDebugString(): String = withMethodTraceVisitor { accept(it) }

/** Get the line number of a node, or -1 if unknown */
val AbstractInsnNode.lineNumber: Int
	get() {
		var node: AbstractInsnNode? = this
		while (true) {
			when (node) {
				null -> return -1
				is LineNumberNode -> return node.line
				else -> node = node.previous
			}
		}
	}

/** A dummy type representing the null value, as used by [AbstractInsnNode.getResultType]. */
private val NULL_TYPE: Type = Type.getObjectType("null")

/**
 * Get the result of evaluating an instruction.
 *
 * @return The result type, or `null` if this expression has no result.
 */
fun AbstractInsnNode.getResultType(): Type? = when (this.opcode) {
	ACONST_NULL -> NULL_TYPE
	ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5, BIPUSH, SIPUSH, // Constants
	INEG, IINC, L2I, F2I, D2I, I2B, I2C, I2S, IADD, ISUB, IMUL, IDIV, IREM, ISHL, ISHR, IUSHR, IAND, IOR, IXOR, // Operators
	ILOAD, IALOAD, BALOAD, CALOAD, SALOAD, ARRAYLENGTH, // Variable/array access
	LCMP, FCMPG, FCMPL, DCMPG, DCMPL,
	-> Type.INT_TYPE

	LCONST_0, LCONST_1, // Constants
	LNEG, I2L, F2L, D2L, LADD, LSUB, LMUL, LDIV, LREM, LSHL, LSHR, LUSHR, LAND, LOR, LXOR, // Operators
	LLOAD, LALOAD, // Variable/array access
	-> Type.LONG_TYPE

	FCONST_0, FCONST_1, FCONST_2, // Constants
	FNEG, I2F, L2F, D2F, FADD, FSUB, FMUL, FDIV, FREM, // Operators
	FLOAD, FALOAD, // Variable/array access
	-> Type.FLOAT_TYPE

	DCONST_0, DCONST_1,  // Constants
	DNEG, I2D, L2D, F2D, DADD, DSUB, DMUL, DDIV, DREM, // Operators
	DLOAD, DALOAD, // Variable/array access
	-> Type.DOUBLE_TYPE

	LDC -> {
		when (val cst = (this as LdcInsnNode).cst) {
			is Int -> Type.INT_TYPE
			is Float -> Type.FLOAT_TYPE
			is Long -> Type.LONG_TYPE
			is Double -> Type.DOUBLE_TYPE
			is String -> Type.getObjectType("java/lang/String")
			is Type -> when (cst.sort) {
				Type.ARRAY, Type.OBJECT -> Type.getObjectType("java/lang/Class")
				Type.METHOD -> Type.getObjectType("java/lang/invoke/MethodType")
				else -> throw IllegalArgumentException("Unknown constant $cst")
			}

			is Handle -> Type.getObjectType("java/lang/invoke/MethodType")
			is ConstantDynamic -> Type.getObjectType(cst.descriptor)
			else -> throw IllegalArgumentException("Unknown constant $cst")
		}
	}

	NEWARRAY -> {
		when ((this as IntInsnNode).operand) {
			T_BYTE -> Type.getType("[B")
			T_INT -> Type.getType("[I")
			else -> throw IllegalStateException("Unhandled opcode ${asDebugString()}")
		}
	}

	ANEWARRAY -> Type.getType("[L${(this as TypeInsnNode).desc};")

	GETSTATIC, GETFIELD -> Type.getType((this as FieldInsnNode).desc)
	NEW, CHECKCAST -> Type.getObjectType((this as TypeInsnNode).desc)
	INVOKEVIRTUAL, INVOKEINTERFACE, INVOKESTATIC, INVOKESPECIAL -> Type.getReturnType((this as MethodInsnNode).desc)
	INVOKEDYNAMIC -> Type.getReturnType((this as InvokeDynamicInsnNode).desc)
	INSTANCEOF -> Type.INT_TYPE

	IFEQ, IFNE, IFLT, IFGT, IFLE, IFGE, IFNULL, IFNONNULL, // Unary comparison
	IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, // Binary comparison
	TABLESWITCH, LOOKUPSWITCH,
	RETURN, IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, // Returns
	MONITORENTER, MONITOREXIT, // Monitors
	PUTSTATIC, PUTFIELD, // Setters
	IASTORE, LASTORE, FASTORE, DASTORE, BASTORE, CASTORE, SASTORE, AASTORE, // Array updates
	ATHROW, // Exceptions!
	-> null

	else -> throw IllegalStateException("Unhandled opcode ${asDebugString()}")
}

/** Check if this instruction loads a constant. */
fun AbstractInsnNode.isConstant(): Boolean = when (opcode) {
	ACONST_NULL,
	ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5, BIPUSH, SIPUSH,
	LCONST_0, LCONST_1,
	FCONST_0, FCONST_1, FCONST_2,
	DCONST_0, DCONST_1,
	LDC,
	-> true

	else -> false
}

/** Check if this is a reference type (an object or array). */
val Type.isReference: Boolean
	get() = when (sort) {
		Type.OBJECT, Type.ARRAY -> true
		Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT, Type.FLOAT, Type.DOUBLE, Type.LONG -> false
		else -> throw IllegalArgumentException("$this is not a value type")
	}

/** Get the opcode to load the default/null value for type.
 *
 * @see Type.getOpcode for the other cases.
 */
fun Type.getDefaultOpcode(): Int = when (sort) {
	Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> ICONST_0
	Type.FLOAT -> FCONST_0
	Type.DOUBLE -> DCONST_0
	Type.LONG -> LCONST_0
	Type.OBJECT, Type.ARRAY -> ACONST_NULL
	else -> throw IllegalArgumentException("$this is not a value type")
}

/** Load a constant int, using the smaller instructions where possible. */
fun MethodVisitor.visitLoadInt(i: Int) = when (i) {
	-1 -> visitInsn(ICONST_M1)
	0 -> visitInsn(ICONST_0)
	1 -> visitInsn(ICONST_1)
	2 -> visitInsn(ICONST_2)
	3 -> visitInsn(ICONST_3)
	4 -> visitInsn(ICONST_4)
	5 -> visitInsn(ICONST_5)
	else -> visitLdcInsn(i)
}

@Suppress("NOTHING_TO_INLINE")
inline fun logger(noinline fn: () -> Unit): Logger =
	LoggerFactory.getLogger(fn.javaClass.toString().substringBefore("Kt$"))
