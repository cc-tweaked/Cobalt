package cc.tweaked.cobalt.build.coroutine

import cc.tweaked.cobalt.build.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.analysis.Interpreter
import org.objectweb.asm.tree.analysis.Value
import org.objectweb.asm.util.Printer

/**
 * The definition point of a value (both stored as a local or on the stack).
 *
 * This is used along with ASM's [Analyzer] and our [DefinitionInterpreter] to form not-really-SSA form for the values
 * involved in this function.
 */
private sealed interface Definition {
	/** An value which is currently unset/undefined. */
	object Unset : Definition {
		override fun toString(): String = "Unset"
		override fun toStringShort(): String = "."
	}

	/**
	 * An input argument to this function.
	 *
	 * Note this is **NOT** the same as the local at position [idx], as it may have been mutated at a later stage.
	 */
	data class Argument(val idx: Int) : Definition {
		override fun toString(): String = "Arg($idx)"
		override fun toStringShort(): String = "A$idx"
	}

	data class Exception(val label: LabelNode) : Definition {
		override fun toStringShort(): String = "E"
	}

	/** A value with a simple known definition. */
	data class Value(val insn: AbstractInsnNode) : Definition {
		override fun toString(): String = "<${insn.asDebugString()}>"
		override fun toStringShort(): String = when (insn) {
			is MethodInsnNode -> "f()"
			is VarInsnNode -> "V${insn.`var`}"
			else -> "<${Printer.OPCODES[insn.opcode]}>"
		}
	}

	/** A special case of [Value], a load from a local variable whose value was [definition] at the time of the load. */
	data class Load(val insn: VarInsnNode, val definition: Definition) : Definition {
		override fun toString(): String = "Load(${insn.asDebugString()} <- $definition)"
		override fun toStringShort(): String = "V${insn.`var`}"
	}

	/** A special case of [Value], the result of a [GETFIELD] operation on the [source] object. */
	data class GetField(val insn: FieldInsnNode, val source: Definition) : Definition {
		override fun toString(): String = "$source.${insn.name}"
		override fun toStringShort(): String = "${source.toStringShort()}.${insn.name}"
	}

	/**
	 * A value with multiple definitions, for instance the result of a ternary `x ? y : z`.
	 *
	 * We don't really need proper phi nodes here, however it's useful to be able to distinguish between different
	 * "unknown" values.
	 */
	data class Phi(val values: Set<Definition>) : Definition {
		override fun toString(): String = "Phi($values)"
		override fun toStringShort(): String = "φ"
	}

	fun toStringShort(): String
}

/**
 * A [Value] implementation which tracks both the [Type] and [Definition].
 */
private class DefinedValue(val type: Type, val definition: Definition) : Value {
	override fun getSize(): Int = type.size

	override fun toString(): String = "Value($definition : $type)"

	fun toStringShort(): String = when (definition) {
		is Definition.Unset -> definition.toStringShort()
		else -> if (type.isReference) "O" else type.descriptor
	}
}

/**
 * An [Interpreter] which generates [DefinedValue]s.
 *
 * The implementation here is fairly rote - we have some special casing to generate more specialised [Definition] terms,
 * but otherwise it's very similar to other [Interpreter] implementations.
 */
private object DefinitionInterpreter : Interpreter<DefinedValue>(ASM9) {
	private val UNKNOWN = Type.getObjectType("unset")

	override fun newValue(type: Type): DefinedValue = throw IllegalStateException("newValue should never be called")

	// Our basic factory methods for opaque values.
	override fun newEmptyValue(local: Int): DefinedValue = DefinedValue(UNKNOWN, Definition.Unset)
	override fun newParameterValue(isInstanceMethod: Boolean, local: Int, type: Type): DefinedValue = DefinedValue(type, Definition.Argument(local))
	override fun newReturnTypeValue(type: Type): DefinedValue? = if (type == Type.VOID_TYPE) null else DefinedValue(type, Definition.Unset)
	override fun newExceptionValue(tryCatchBlockNode: TryCatchBlockNode, handlerFrame: Frame<DefinedValue>, exceptionType: Type): DefinedValue = DefinedValue(exceptionType, Definition.Exception(tryCatchBlockNode.handler))

	private fun toValue(insn: AbstractInsnNode): DefinedValue? {
		val type = insn.getResultType() ?: return null
		return DefinedValue(type, Definition.Value(insn))
	}

	override fun merge(value1: DefinedValue, value2: DefinedValue): DefinedValue = when {
		// Incompatible definitions just put an undefined value on the stack. This is somewhat imprecise, but appears
		// good enough right now.
		value1.type != value2.type -> DefinedValue(UNKNOWN, Definition.Unset)
		// If the definition is the same, keep it.
		value1.definition == value2.definition -> value1
		// Otherwise take the union between the two and create a Phi node. There's some optimisations here to avoid
		// creating new nodes if not needed, but it's otherwise quite simple.
		else -> DefinedValue(value1.type, merge(value1.definition, value2.definition))
	}

	private fun merge(value1: Definition, value2: Definition): Definition = when {
		value1 is Definition.Load && value2 is Definition.Load && value1.insn == value2.insn ->
			Definition.Load(value1.insn, merge(value1.definition, value2.definition))

		value1 is Definition.Phi && value2 !is Definition.Phi && value1.values.contains(value2) -> value1
		value1 !is Definition.Phi && value2 is Definition.Phi && value2.values.contains(value1) -> value2

		else -> {
			val defs = HashSet<Definition>(2)
			if (value1 is Definition.Phi) defs.addAll(value1.values) else defs.add(value1)
			if (value2 is Definition.Phi) defs.addAll(value2.values) else defs.add(value2)
			if (defs.size == 1) defs.first() else Definition.Phi(defs)
		}
	}

	override fun copyOperation(insn: AbstractInsnNode, value: DefinedValue): DefinedValue = when (insn.opcode) {
		ISTORE, LSTORE, FSTORE, DSTORE, ASTORE, DUP, DUP2, DUP_X1, DUP_X2, DUP2_X1, DUP2_X2, SWAP -> value
		ILOAD, LLOAD, FLOAD, DLOAD, ALOAD -> DefinedValue(value.type, Definition.Load(insn as VarInsnNode, value.definition))
		else -> throw IllegalStateException("Unhandled opcode ${insn.asDebugString()}")
	}

	override fun newOperation(insn: AbstractInsnNode): DefinedValue = toValue(insn)!!
	override fun returnOperation(insn: AbstractInsnNode, value: DefinedValue?, expected: DefinedValue?) = Unit
	override fun unaryOperation(insn: AbstractInsnNode, value: DefinedValue): DefinedValue? = when (insn.opcode) {
		GETFIELD -> DefinedValue(insn.getResultType()!!, Definition.GetField(insn as FieldInsnNode, value.definition))
		else -> toValue(insn)
	}

	override fun binaryOperation(insn: AbstractInsnNode, value1: DefinedValue, value2: DefinedValue): DefinedValue? =
		when (insn.opcode) {
			AALOAD -> {
				val type = value1.type
				if (type.sort != Type.ARRAY) throw IllegalStateException("Not an array.")
				DefinedValue(type.elementType, Definition.Value(insn))
			}

			else -> toValue(insn)
		}

	override fun ternaryOperation(insn: AbstractInsnNode, value1: DefinedValue, value2: DefinedValue, value3: DefinedValue): DefinedValue? = toValue(insn)
	override fun naryOperation(insn: AbstractInsnNode, values: MutableList<out DefinedValue>): DefinedValue? = toValue(insn)
}

/** A stack value which can be reconstructed from existing local state. */
sealed interface StackValue {
	/** A value which can be loaded from a constant. */
	class Constant(val insn: AbstractInsnNode) : StackValue {
		override fun toString(): String = "[${insn.asDebugString()}]"
	}

	/** Load this value from a local variable. */
	class Local(val insn: VarInsnNode) : StackValue {
		override fun toString(): String = "Local(${insn.`var`})"
	}

	/** Load a field from a value. */
	class Field(val owner: String, val name: String, val desc: String, val ownerValue: StackValue) : StackValue {
		override fun toString(): String = "$ownerValue.$name"
	}
}

/** A local value which may be saved. */
sealed interface LocalValue {
	/** This local is neither needs to be saved or restored. */
	object Skip : LocalValue

	/** This local should be saved. */
	data class Saved(val ty: Type) : LocalValue
}

/**
 * A point in the program where we may yield, and the information needed to recover after this yield.
 */
interface YieldPoint {
	/** The label that occurs immediately before [yieldAt]. */
	val label: LabelNode

	/** The method which may yield. */
	val yieldAt: MethodInsnNode

	/** The type of yield of [yieldAt]. */
	val yieldType: YieldType

	/**
	 * A list of local values at this yield point.
	 *
	 * This will include locals whose slots are taken up by 2-wide values (doubles and longs).
	 */
	val locals: List<LocalValue>

	/**
	 * The list of values to be pushed to the stack.
	 */
	val stack: List<StackValue>
}

/** The internal implementation of [YieldPoint]. */
private class YieldPointImpl(
	override val label: LabelNode,
	override val yieldAt: MethodInsnNode,
	override val yieldType: YieldType,
	override val stack: List<StackValue>,
) : YieldPoint {
	override lateinit var locals: List<LocalValue>
}

/**
 * Filter down our list of basic blocks to those which may actually yield/resume, then find how to recover at this
 * position.
 */
fun findYieldPoints(className: String, method: MethodNode, blocks: List<Block>, definitions: DefinitionData): List<YieldPoint> {
	if (method.maxLocals > 64) throw UnsupportedOperationException("More than 64 locals are not currently supported")

	// We track the liveness of each local variable at the start of each block. This is represented as a basic bitmask,
	// where local i is live iff the i-th bit is set.
	// We don't compute liveness until later on in the functoion, but we want to force some variables to be live (if
	// they're needed in the recovery code), so we define the array now.
	val liveness = LongArray(blocks.size)

	// We now run our above interpreter to find the state of the locals and stack at the start of each block.
	val analyzer = Analyzer(DefinitionInterpreter)
	val frames = analyzer.analyze(className, method)

	val yieldPoints = mutableListOf<YieldPointImpl>()

	// For each yield point, we inspect the stack and find how to reconstruct its state.
	for (block in blocks) {
		val yieldAt = block.yieldAt ?: continue
		val yieldType = block.yieldType!!
		val frame = frames[method.instructions.indexOf(yieldAt)]
		val argSize = Type.getArgumentTypes(yieldAt.desc).size
		val noArgStackSize = frame.stackSize - argSize

		// Print an error message about why we cannot save the current frame.
		fun fail(details: String): Nothing {
			val message = StringBuilder()
			message.append("$className.${method.name} at line ${yieldAt.lineNumber}: Cannot recover stack: ").append(details).append('\n')

			message.append("Locals:\n")
			var localIdx = 0
			while (localIdx < frame.locals) {
				val value = frame.getLocal(localIdx)
				message.append(" - ").append(value.toString()).append("\n")
				localIdx += value.size
			}

			message.append("Stack:\n")
			for (stackIdx in 0 until noArgStackSize) {
				val value = frame.getStack(stackIdx)
				message.append(" - ").append(value.toString()).append("\n")
			}

			throw UnsupportedConstruct(message.toString())
		}

		// Find a strategy to recreate each value currently on the stack. Note, we *only* allow recreating stack values,
		// we do not attempt to persist them. While this is quite restrictive (it does mean intermediate values need to
		// be saved to locals), it massively simplifies the implementation.
		fun recoverValue(def: Definition): StackValue = when (def) {
			is Definition.Unset -> fail("Cannot have an unset value on the stack. This is a bug.")
			// Impossible, as this would have gone through a Load first.
			is Definition.Argument -> fail("Cannot have an argument on the stack. This is a bug.")

			is Definition.Load -> {
				// Ensure the current value of our local is consistent with the expected value.
				val local = def.insn.`var`
				if (local >= frame.locals || frame.getLocal(local).definition != def.definition) {
					fail("Accessing local $local, but its definition has been invalidated.")
				}

				// Forcibly mark this local as live, so that we'll save it.
				liveness[block.index] = liveness[block.index] or (1L shl local)

				StackValue.Local(def.insn)
			}

			is Definition.GetField -> {
				if (!definitions.isFieldFinal(def.insn.owner, def.insn.name, def.insn.desc)) {
					fail("Accessing field ${def.insn.name}.")
				}

				StackValue.Field(def.insn.owner, def.insn.name, def.insn.desc, recoverValue(def.source))
			}

			is Definition.Value -> when {
				def.insn.isConstant() -> StackValue.Constant(def.insn)
				// GETSTATIC on a final field is basically a constant, right??
				def.insn.opcode == GETSTATIC && definitions.isFieldFinal((def.insn as FieldInsnNode).owner, def.insn.name, def.insn.desc)
				-> StackValue.Constant(def.insn)

				else -> fail("$def is a complex expression")
			}

			is Definition.Exception -> fail("$def is a-provided exception.")

			is Definition.Phi -> fail("$def is a complex expression.")
		}

		// Find out how to store each stack value.
		val stack = mutableListOf<StackValue>()
		for (i in 0 until noArgStackSize) stack.add(recoverValue(frame.getStack(i).definition))

		yieldPoints.add(YieldPointImpl(block.first, yieldAt, yieldType, stack))
	}

	// We first compute liveness for our local variables. We do this with a standard fixed-point algorithm, propagating
	// liveness from the end of the block towards the beginning. We loop over blocks in post order - technically not
	// needed, but should reduce the iterations before reaching a fixed point.
	val orderedBlocks = Block.asPostOrder(blocks)
	do {
		var changed = false
		for (block in orderedBlocks) {
			val currentLiveness = liveness[block.index]
			var newLiveness = currentLiveness
			for (successor in block.outgoing) newLiveness = newLiveness or liveness[successor.index]

			for (insn in block.reversed()) {
				if (insn !is VarInsnNode) continue

				newLiveness = when (insn.opcode) {
					ISTORE, LSTORE, FSTORE, DSTORE, ASTORE -> newLiveness and (1L shl insn.`var`).inv()
					ILOAD, LLOAD, FLOAD, DLOAD, ALOAD -> newLiveness or (1L shl insn.`var`)
					else -> throw IllegalStateException("Unknown opcode ${insn.asDebugString()}")
				}
			}

			if (currentLiveness != newLiveness) {
				liveness[block.index] = newLiveness
				changed = true
			}
		}
	} while (changed)

	// Now we've computed liveness, go through and work out which locals we need to restore.
	var pointIdx = 0
	for (block in blocks) {
		val blockLiveness = liveness[block.index]
		val frame = frames[method.instructions.indexOf(block.first)]
		if (block.yieldAt == null) continue

		val locals = mutableListOf<LocalValue>()

		var i = 0
		while (i < frame.locals) {
			val local = frame.getLocal(i)

			val value = when {
				// Don't need to save "this"
				i == 0 && (method.access and ACC_STATIC) == 0 -> LocalValue.Skip
				// Unset values don't need to be defined.
				local.definition is Definition.Unset -> LocalValue.Skip
				// Similarly, dead values can also be skipped.
				(blockLiveness and (1L shl i)) == 0L -> LocalValue.Skip
				// Otherwise we need to save this value.
				else -> LocalValue.Saved(local.type)
			}
			locals.add(value)

			when (local.size) {
				1 -> {}
				2 -> locals.add(LocalValue.Skip)
				else -> throw IllegalStateException("Unexpected local of size ${local.size}")
			}
			i += local.size
		}

		yieldPoints[pointIdx].locals = locals

		pointIdx += 1
	}

	return yieldPoints
}
