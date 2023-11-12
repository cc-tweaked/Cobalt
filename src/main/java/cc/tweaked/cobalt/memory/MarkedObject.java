package cc.tweaked.cobalt.memory;

public interface MarkedObject extends AllocatedObject {
	int IGNORE = 2;

	boolean markObject(byte mask);
}
