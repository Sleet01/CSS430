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
     * @return  free    int, free page index (or -1 if no free pages)
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

    /**
     * @brief   Find a page to use (either for reading or for writing)
     * @pre     Cache table has been initialized
     * @post    The selected page will either be free, or be written back before it is selected
     * @return  index   int, cache table index of the page to use based on enhanced second-chance algo.
     *                  index will either be A) an existing free (unused) page, or B) the lowest-score
     *                  used page within the cache.
     */
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

    /**
     * @brief   Select a victim via two-pass Enhanced Second-Chance algorithm.
     *          Presupposes that there are no free blocks.
     * @pre     All blocks are used, and have their reference and dirty bits set appropriately
     * @post    Any reference bits will be unset.
     * @return  index       int, the first lowest-state index in the table.
     */
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

            if (victim.block != -1 && victim.dirtybit) {
                SysLib.rawwrite(victim.block, victim.data);
                victim.dirtybit = false;
            }
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


        // Otherwise, check if the requested block is in the cache
        for (int i = 0; i < this.cacheSize; ++i){

            // If the block was in the cache, copy it into the buffer and update the reference bit of that
            // cache block.
            if (cacheTable[i].block == blockId){
                //SysLib.cout(String.format("<< CACHE >> read: found cached at index %d", i));
                System.arraycopy(cacheTable[i].data, 0, buffer, 0, 512);
                cacheTable[i].refbit = true;
                read = true;
                break;
            }
        }

        // If the block was *not* in the cache, load it from the disk into cache (and the buffer)
        if(!read){
            read = bufferFromDisk(blockId, buffer);
        }

        // Should only be false if there were a catastrophic failure during disk read
        return read;
    }

    /**
     * @brief       Helper function that reads in a block from the disk backing store while caching a copy
     * @pre         blockId is a valid ID # between 0 and 1000 (checked prior to calling this method)
     * @post        The desired block will be read from disk into both the buffer and the cache
     * @param[in] blockId       int, from 0 to 999, indicating on-disk block number desired
     * @param[out] buffer        byte[] that stores the data from disk for immediate return.
     *                           If buffer is a zero-length array, do not load it with data (for write caching)
     * @return readFromDisk     boolean; only false if there is some disk error
     */
    private boolean bufferFromDisk(int blockId, byte[] buffer) {

        boolean readFromDisk;
        int victimId = selectVictim();

        // Attempt to read from disk; stores "true" if rawread succeeds.
        readFromDisk = ( SysLib.rawread( blockId, cacheTable[victimId].data) == Kernel.OK );

        // If successful, also fill buffer with retrieved data and set reference bit of cache entry.
        // If a zero-length byte[] is passed in, however, the buffer will not be filled.
        if(readFromDisk){
            if(buffer.length != 0) {
                System.arraycopy(cacheTable[victimId].data, 0, buffer, 0, 512);
                //buffer = cacheTable[victimId].data.clone();
            }
            cacheTable[victimId].refbit = true;
        }

        return readFromDisk;
    }


    /**
     * @brief   Attempt to write a data block to the buffer; if not found, load it in then write
     * @pre     Buffer is initialized, and desired block exists on disk
     * @post    Block is stored in the buffer, possibly replacing another paged block, and changes written to it
     * @param blockId       int index of block (on disk) to try to write into
     * @param buffer        byte[] to read the desired data in from
     * @return wrote        boolean, only false if blockId was invalid
     */
    public synchronized boolean write(int blockId, byte buffer[]) {

        boolean wrote = false;

        // Trivial case: blockId is invalid
        if(blockId < 0 || blockId >= 1000) {return wrote;}

        // Store any found or selected cache table index
        int index = -1;

        // Check if the requested block is in the cache
        for (int i = 0; i < this.cacheSize; ++i){

            // If the block was in the cache, copy the buffer into it and update the reference and dirty bits of that
            // cache block.
            if (cacheTable[i].block == blockId){
                index = i;
                break;
            }
        }

        // If the block was *not* in the cache, select a victim to write back and replace
        if(index == -1){

            // Select a viable victim to replace; this also causes the victim to be written back so we can
            // re-use this block with impunity
            index = selectVictim();

        }

        // Only write anything into the cache table if we got a valid index; otherwise, do nothing and return a failure
        if(index != -1){

            // Whether the selected index is used but cached; empty; or a nominated victim, we are going to
            // overwrite its contents and mark it as a referenced, dirty cache block.
            cacheTable[index].block = blockId;
            System.arraycopy(buffer, 0, cacheTable[index].data, 0, 512);
            //cacheTable[index].data = buffer.clone();
            cacheTable[index].refbit = true;
            cacheTable[index].dirtybit = true;
            wrote = true;

        }

        return wrote;
    }

    /**
     * @brief   Write out all dirty blocks in cache and force write-out to DISK file
     * @pre     Cache has been initialized
     * @post    Any used, dirty blocks will be written back, their dirty bit un-set.
     */
    public synchronized void sync() {

        // Write back all pages (that are valid and dirty)
        for(int i = 0; i < cacheSize; ++i){
            writeBackPage(i);
        }
        // Call Disk.class' sync() method via SysLib to force write-out to backing "disk" file
        SysLib.sync();
    }

    /**
     * @brief   Sync, then clear cache for re-use
     * @pre     Cache has been initialized
     * @post    Any dirty blocks are written to disk, and all cache blocks are re-set to unused
     */
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

        /**
         * @brief       Inner class CacheEntry's constructor.
         * @param blockSize     int, number of data bytes in each block on the current system instance
         */
        public CacheEntry(int blockSize){
            data = new byte[blockSize];
        }

    }
}
