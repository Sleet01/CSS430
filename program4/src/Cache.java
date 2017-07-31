import java.util.*;
import java.util.concurrent.*;

/**
 * @author  Martin L. Metke
 * @date    2017/07/30
 *
 * The Cache class provides a cached disk access interface for on-demand paging.
 */
public class Cache {

    private Entry[] pageTable = null;

    public Cache(int blockSize, int cacheBlocks) {
    }

    private class Entry {
    }


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
