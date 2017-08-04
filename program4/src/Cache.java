import java.util.*;
import java.util.concurrent.*;

/**
 * @author  Martin L. Metke
 * @date    2017/07/30
 *
 * The Cache class provides a cached disk access interface for on-demand paging.
 */
public class Cache {

    private final int cacheSize;
    private final int bSize;

    private CacheEntry[] cacheTable = null;

    /**
     * @brief   Constructor for Cache class.  Creates the cache store and initializes page table
     * @param blockSize     int, size of a disk block (and corresponding cache page)
     * @param cacheBlocks   int, count of disk blocks / pages that can be cached at once
     */
    public Cache(int blockSize, int cacheBlocks) {

        cacheSize = cacheBlocks;
        bSize = blockSize;
        cacheTable = new CacheEntry[cacheSize];

        for (int i = 0; i < cacheSize; ++i){
            cacheTable[i] = new CacheEntry(bSize);
        }
    }

    /**
     * @brief   Method to find a free (unallocated) page in the buffer
     * @return
     */
    private int findFreePage() {

        int free = -1;

        for (int i = 0; i < cacheSize; ++i){
            if(cacheTable[i].block == -1){
                free = i;
                break;
            }
        }

        return free;
    }

    private int selectVictim(){

        int index = findFreePage();

        // If there are no free pages, select a victim
        if (index == -1){

            // Get the index
            index = nextVictim();

            // Request a write-back; only fires if index is dirty
            writeBackPage(index);
        }

        return index;
    }

    private int nextVictim() {
        int index = 0;
        int lastBest = 0b11111111;
        int bitmap;
        CacheEntry current;

        for (int j = 0; j < 2; ++j){
            for (int i = 0; i < cacheSize; ++i){
                current = cacheTable[i];
                bitmap = (current.refbit ? 0b00000010 : 0b00000000);
                bitmap = bitmap & (current.dirtybit ? 0b00000001 : 0b00000000);

                switch (bitmap) {
                    case 0b00:  index = i;
                                return index; // Return as soon as we find a 00 entry
                    case 0b01:  if(lastBest > bitmap) {
                                    lastBest = bitmap;
                                    index = i;
                                }
                                break;
                    case 0b10:
                    case 0b11:  if(lastBest > bitmap) {
                                    lastBest = bitmap;
                                    index = i;
                                }
                                current.refbit = false;
                                break;
                }
            }
        }
        return index;
    }

    /**
     * @brief   Write a specific CacheEntry from the table out to the disk via SysLib
     * @pre     victim is valid
     * @post    victim will be clean, and all changes within its data will be on disk
     * @param victimEntry   int, index of the victim within cacheTable
     */
    private void writeBackPage(int victimEntry) {

        if(victimEntry >= 0 && victimEntry < cacheSize) {

            CacheEntry victim = cacheTable[victimEntry];

            if (victim.dirtybit) {
                SysLib.rawwrite(victim.block, victim.data);
            }

            victim.dirtybit = false;
        }
    }

    /**
     * @brief   Attempt to read a data block from buffer; if not found, load it in
     * @pre     Buffer is initialized, and desired block exists on disk
     * @post    Block is stored in the buffer, possibly replacing another paged block
     * @param blockId       int index of block (on disk) to try to read
     * @param buffer        byte[] to store the desired data in
     * @return read         boolean, only false if blockId was invalid
     */
    public synchronized boolean read(int blockId, byte buffer[]) {

        boolean read = false;

        // Trivial case: blockId is invalid
        if(blockId < 0 || blockId >= 1000) {return read;}


        for (int i = 0; i < this.cacheSize; ++i){

            if (cacheTable[i].block == blockId){
                buffer = cacheTable[i].data.clone();
                cacheTable[i].refbit = true;
                read = true;
                break;
            }
        }

        if(!read){
            read = bufferFromDisk(blockId, buffer);
        }

        return read;
    }

    private boolean bufferFromDisk(int blockId, byte[] buffer) {

        boolean readFromDisk;
        int victimId = selectVictim();

        // Attempt to read from disk
        readFromDisk = ( SysLib.rawread( blockId, cacheTable[victimId].data) == Kernel.OK );

        // If successful, also fill buffer with retrieved data
        if(readFromDisk){
            buffer = cacheTable[victimId].data.clone();
            cacheTable[victimId].refbit = true;
        }

        return readFromDisk;
    }


    public synchronized boolean write(int blockId, byte buffer[]) {
        return false;
    }

    public synchronized void sync() {

        for(int i = 0; i < cacheSize; ++i){
            writeBackPage(i);
        }
        SysLib.sync();
    }

    public synchronized void flush() {

        this.sync();

        for (int i = 0; i < cacheSize; ++i){
            cacheTable[i] = new CacheEntry(bSize);
        }
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
