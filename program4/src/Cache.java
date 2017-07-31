import java.util.*;
import java.util.concurrent.*;

/**
 * @author  Martin L. Metke
 * @date    2017/07/30
 *
 * The Cache class provides a cached disk access interface for on-demand paging.
 */
public class Cache {

    private static final int CACHE_SIZE = 10;

    private CacheEntry[] cacheTable = null;

    /**
     * @brief   Constructor for Cache class.  Creates the cache store and initializes page table
     * @param blockSize     int, size of a disk block (and corresponding cache page)
     * @param cacheBlocks   int, count of disk blocks / pages that can be cached at once
     */
    public Cache(int blockSize, int cacheBlocks) {

        cacheTable = new CacheEntry[cacheBlocks];

        for (int i = 0; i < cacheBlocks; ++i){
            cacheTable[i] = new CacheEntry(blockSize);
        }
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

    /**
     * Inner Class with nothing but public members; essentially as Struct-y a
     * data object as Java can encompass.
     */
    private class CacheEntry {

        public int block = -1;
        public boolean refbit = false;
        public boolean dirtybit = false;
        public byte[] data;

        public CacheEntry(int blockSize){
            data = new byte[blockSize];
        }

    }
}
