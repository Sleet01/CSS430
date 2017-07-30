import java.util.*;
import java.util.concurrent.*;

public class Cache {

    public Cache(int blockSize, int cacheBlocks) {
    }


    private class Entry {
    }

    private Entry[] pageTable = null;

    private int findFreePage() {
        return -1;
    }

    private int nextVictim() {
        return -1;
    }

    private void writeBack(int victimEntry) {
    }

    public synchronized boolean read(int blockId, byte buffer[]) {
        return false;
    }

    public synchronized boolean write(int blockId, byte buffer[]) {
        return false;
    }

    public synchronized void sync() {
    }

    public synchronized void flush() {
    }
}
