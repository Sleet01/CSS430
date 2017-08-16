import java.util.ArrayList;
import java.util.List;

class Superblock {
    private final static int[] fieldSizes = {4, 4, 4}; // Size of fields needed to store Superblock on disk

    public int totalBlocks; // the number of disk blocks
    public int totalInodes; // the number of inodes
    public int freeList;    // the block number of the free list's head


    public Superblock( int diskSize ) {
        this.totalBlocks = diskSize;
        this.totalInodes = 64;
        this.freeList = (int)Math.ceil(this.totalInodes / 16.0) + 1; // First free block should be first block after Inodes
    }

    public Superblock( int diskSize, int inodeCount ) {
        this.totalBlocks = diskSize;
        this.totalInodes = inodeCount;
        this.freeList = (int)Math.ceil(this.totalInodes / 16.0) + 1; // First free block should be first block after Inodes
    }

    public Superblock( short block) {
        // Prep to load
        List<Object> fields = SysLib.disk2List(block, 0, fieldSizes);

        if(fields.get(0) != null) { // read-in from disk has succeeded
            this.totalBlocks = (int) fields.get(0);
            this.totalInodes = (int) fields.get(1);
            this.freeList = (int) fields.get(2);
        }
        else{ // Something has gone wrong; initialize with useless values
            this.totalBlocks = 0;
            this.totalInodes = 0;
            this.freeList = 1;
        }
    }

    public short getNextFree(){
        int [] header = {2};
        short next = (short)freeList;

        short nextNext = (short)(SysLib.disk2List(next, 0, header)).get(0);
        freeList = nextNext;

        return next;
    }

    public int toDisk(){
        List<Object> fields = new ArrayList<Object>(fieldSizes.length);
        fields.set(0, this.totalBlocks);
        fields.set(1, this.totalInodes);
        fields.set(2, this.freeList);

        return SysLib.list2Disk(fields, fieldSizes, (short)0, 0);
    }
}