package cc.tweaked.cobalt.build.coroutine

import cc.tweaked.cobalt.build.UnsupportedConstruct
import cc.tweaked.cobalt.build.quoteForGraphViz
import cc.tweaked.cobalt.build.withMethodTraceVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.GOTO
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.Interpreter

/** A basic block in the function's CFG. */
interface Block : Iterable<AbstractInsnNode> {
	/** The index of this block. */
	val index: Int

	/** The first node in our block. This will be a label node. */
	val first: LabelNode

	/** The last node in our block. */
	val last: AbstractInsnNode

	/**
	 * The node in this block which may yield.
	 *
	 * If set, this will be the first non-metadata (so non-[LabelNode], [LineNumberNode], [FrameNode]) node in the
	 * block.
	 */
	val yieldAt: MethodInsnNode?

	/**
	 * The type of yield at [yieldAt]. This will be non-null iff [yieldAt] is.
	 */
	val yieldType: YieldType?

	/** The set of all incoming blocks. */
	val incoming: Set<Block>

	/** The set of all outgoing blocks. */
	val outgoing: Set<Block>

	/** Get a debugging representation of this block's instructions. */
	fun dumpInstructions(): String = withMethodTraceVisitor {
		for (insn in this) insn.accept(it)
	}

	/** Iterate over the instructions in this block. */
	override fun iterator(): Iterator<AbstractInsnNode> = object : Iterator<AbstractInsnNode> {
		var next: AbstractInsnNode? = first
		override fun hasNext(): Boolean = next != null
		override fun next(): AbstractInsnNode {
			val node = next!!
			next = if (node == last) null else node.next
			return node
		}
	}

	/** Iterate over the instructions in this block in reverse order. */
	fun reversed(): Iterable<AbstractInsnNode> = object : Iterable<AbstractInsnNode> {
		override fun iterator(): Iterator<AbstractInsnNode> = object : Iterator<AbstractInsnNode> {
			var prev: AbstractInsnNode? = last
			override fun hasNext(): Boolean = prev != null
			override fun next(): AbstractInsnNode {
				val node = prev!!
				prev = if (node == first) null else node.previous
				return node
			}
		}
	}

	companion object {
		/**
		 * Dump a list of blocks to GraphViz's dot representation. This prints directly to stdout.
		 *
		 * @param blocks The function's list of blocks.
		 * @param prefix An optional prefix for each node in the graph.
		 */
		fun dump(blocks: List<Block>, prefix: String = "") {
			for (block in blocks) {
				print("$prefix${block.index} [label=${block.dumpInstructions().quoteForGraphViz()}, shape=box")
				if (block.yieldAt != null) print(", color=blue")
				println("];")

				for (out in block.outgoing) println("$prefix${block.index} -> $prefix${out.index};")
			}
		}

		/**
		 * Convert a list of blocks to be in post-order traversal order.
		 *
		 * The first block in the list is assumed to be the entry point.
		 */
		fun asPostOrder(blocks: List<Block>): Collection<Block> {
			val orderedBlocks = mutableListOf<Block>()
			fun visitNodes(visited: MutableSet<Block>, block: Block) {
				for (child in block.outgoing) {
					if (visited.add(child)) visitNodes(visited, child)
				}
				orderedBlocks.add(block)
			}
			visitNodes(mutableSetOf(blocks[0]), blocks[0])
			assert(orderedBlocks.size == blocks.size)
			return orderedBlocks
		}
	}
}

/** Our internal implementation of [Block], which is built up over the course of [extractBlocks]. */
private class MutableBlock(
	override var index: Int,
	override val first: LabelNode, override var last: AbstractInsnNode,
	override var yieldAt: MethodInsnNode? = null, override var yieldType: YieldType? = null,
	override val incoming: MutableSet<MutableBlock> = mutableSetOf(), override val outgoing: MutableSet<MutableBlock> = mutableSetOf(),
) : Block

/** Get or create a label before the given node. */
private fun InsnList.getOrCreateLabel(node: AbstractInsnNode) = if (node is LabelNode) node else {
	val label = Label()
	val labelNode = LabelNode(label)
	label.info = labelNode
	this.insertBefore(node, labelNode)
	labelNode
}

/**
 * Generate a list of basic blocks from a method body.
 *
 * We form basic blocks for each node in the control flow graph (as normal) as well for each point where the program
 * may yield. In this case, the function call which yields will be the first instruction in the block (see
 * [Block.yieldAt]).
 *
 * While using basic blocks is a little overkill (especially seeing as we duplicate much of the work done by ASM's
 * [Analyzer]/[Interpreter]), it makes it much easier to find liveness information in later steps.
 *
 * @param instructions The method's instructions.
 * @param definitions The set of instructions which may yield.
 * @return The list of basic blocks in this function.
 */
fun extractBlocks(instructions: InsnList, definitions: DefinitionData): List<Block> {
	// We perform two pass over our list of instructions. The first time round, we find which program points form the
	// start of a basic block (the entry point, jump targets and the node after a yielding method).
	val blockLabels = HashMap<Label, MutableBlock>()
	fun createBlock(label: LabelNode) =
		blockLabels.computeIfAbsent(label.label) { MutableBlock(0, label, label) }
	createBlock(instructions.getOrCreateLabel(instructions.first))
	for (instr in instructions) {
		when (instr) {
			// If this method may yield, then place it at the start of a block. We either reuse the preceding label or
			// create a new one.
			is MethodInsnNode -> when (val yieldType = definitions.getYieldType(instr.owner, instr.name, instr.desc)) {
				null -> {}
				is YieldType.Unsupported -> throw UnsupportedConstruct("${instr.owner}.${instr.name}${instr.desc} unwinds but cannot be resumed.")
				else -> {
					var prev = instr.previous
					while (prev is FrameNode || prev is LineNumberNode) prev = prev.previous
					val block = createBlock(if (prev is LabelNode) prev else instructions.getOrCreateLabel(instr))
					block.yieldAt = instr
					block.yieldType = yieldType
				}
			}

			// The jump target obviously forms a block. However, as does the next instruction as we may fall through!
			is JumpInsnNode -> {
				createBlock(instr.label)
				if (instr.opcode != GOTO) createBlock(instructions.getOrCreateLabel(instr.next))
			}

			// Lookup and Table switches are easy - just create a block for each label.
			is LookupSwitchInsnNode -> {
				createBlock(instr.dflt)
				for (label in instr.labels) createBlock(label)
			}

			is TableSwitchInsnNode -> {
				createBlock(instr.dflt)
				for (label in instr.labels) createBlock(label)
			}
		}
	}

	// We now perform our second pass - we loop over each instruction and grow the current block. When reaching the end
	// of the block, add appropriate incoming/outgoing edges.
	val blocks: MutableList<MutableBlock> = mutableListOf()
	var block = blockLabels[(instructions.first as LabelNode).label]!!
	fun addEdge(target: Label): MutableBlock {
		val otherBlock = blockLabels[target]!!
		block.outgoing.add(otherBlock)
		otherBlock.incoming.add(block)
		return otherBlock
	}
	for (instr in instructions) {
		// Switch over to a new block if we're at the start of one.
		if (instr is LabelNode) {
			val nextBlock = blockLabels[instr.label]
			if (nextBlock != null) {
				// Handle fallthrough from the previous block to this one. Ewww.
				if (block.outgoing.isEmpty() && block != nextBlock) addEdge(nextBlock.first.label)

				block = nextBlock
				block.index = blocks.size
				blocks.add(block)
			}
		}

		assert(block.outgoing.isEmpty()) { "Block has outgoing edges" }
		block.last = instr

		// Same logic as the previous pass. Yes, the duplication is ugly here!
		when (instr) {
			is JumpInsnNode -> {
				addEdge(instr.label.label)
				if (instr.opcode != GOTO) addEdge((instr.next as LabelNode).label)
			}

			is LookupSwitchInsnNode -> {
				addEdge(instr.dflt.label)
				for (label in instr.labels) addEdge(label.label)
			}

			is TableSwitchInsnNode -> {
				addEdge(instr.dflt.label)
				for (label in instr.labels) addEdge(label.label)
			}
		}
	}

	// Do one quick safety check to ensure our blocks actually contain something.
	for (block in blocks) assert(block.first != block.last) { "Function contains empty blocks" }

	return blocks
}
