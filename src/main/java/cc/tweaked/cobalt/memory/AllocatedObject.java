package cc.tweaked.cobalt.memory;

public interface AllocatedObject {
	long OBJECT_SIZE = 32;
	long POINTER_SIZE = 4;

	long traceObject(MemoryCounter counter, int depth);
}
