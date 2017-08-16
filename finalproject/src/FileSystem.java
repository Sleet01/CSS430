public class FileSystem {
    private Superblock superblock;
    private Directory directory;
    private FileTable filetable;
    private int dataBlockCount;

    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    public FileSystem( int diskBlocks ){
        // default superblock creation and formatting
        superblock = new Superblock( diskBlocks );

        // create new Directory and set root "/" as first entry
        directory = new Directory( superblock.totalInodes );

        // Link directory to new FileTable
        filetable = new FileTable( directory, superblock.totalInodes );

        // Recreate the root entry if one exists
        FileTableEntry dirEnt = open( "/", "r");
        int dirSize = fsize( dirEnt );
        if ( dirSize > 0 ) {
            byte[] dirData = new byte[dirSize];
            read(dirEnt, dirData);
            directory.bytes2directory(dirData);
        }
        close( dirEnt );

        // calculate the total number of blocks available for data from the disk blocks and inode count
        dataBlockCount = (diskBlocks - (1 + (int)Math.ceil(superblock.totalInodes/16.0)));
    }

    public void sync( ) {

    }

    private synchronized boolean initializeSuperblock(Superblock sb){
        boolean initialized = false;
        try{
            initialized = ((sb.toDisk() == Kernel.OK));
        }
        catch (Exception e){
            SysLib.cerr(e.toString());
        }

        return initialized;
    }

    private synchronized boolean initializeDirectory(Directory dir){
        boolean initialized = false;

        try{
            // allocate byte array for the new directory's byte representation
            byte [] dirAsBytes = dir.directory2bytes();
            byte [] buffer = new byte[512];
            int remLength, directCount;
            int indirectCount = 0;

            // Allocate and configure the very first Inode for use by the directory
            Inode dirI = new Inode();
            dirI.length = dirAsBytes.length;    // Total length of the initial dir, *should* be 256 + 1

            // Figure out how many data blocks, of both direct and indirect type, are needed
            directCount = (int)Math.ceil(dirI.length/512); // Total number of blocks needed
            if(directCount > 11){
                indirectCount = directCount - 11;
                directCount = 11;
            }

            // Prepare to write dir to disk.  It *should* be smaller than 512b, but that's not
            // guaranteed.  So we need an approximation of the Inode write-out process (may move this later)
            // For every block required to store the Directory's initial status, write out 512 bytes
            // in a free data block (and update the freeList)
            for(int i = 0; i < directCount; ++i){
                // Assign next free block to this Inode
                dirI.direct[i] = superblock.getNextFree();

                // Check remaining length of dirAsBytes to prevent underrun Exceptions
                remLength = (dirI.length-(i*512) >= 512) ? 512 : dirI.length - (i * 512);

                // Read next section of dirAsBytes into buffer for writing;
                System.arraycopy(dirAsBytes, 512 * i, buffer, 0, remLength);

                // Write out the current 512-byte chunk
                SysLib.rawwrite(dirI.direct[i], buffer);
            }

            // If there is (somehow) a need for indirect data blocks for the directory, we must
            // A) Allocate a data block # for the indirect block
            // B) Allocate a buffer to store entries for that indirect block
            // C) Allocate one (or more) data block numbers for the data blocks the indirect block points at
            if(indirectCount != 0){
                dirI.indirect = superblock.getNextFree();

                byte[] indirectBlock = new byte[512];       // Allocate a block to hold all indirect data blocks

                short nextDataBlock;

                for(int j = 0; j < indirectCount; ++j){

                    // C) allocate another data block for the indirectly-pointed-to data
                    nextDataBlock = superblock.getNextFree();

                    // Load the new data block location into indirectBlock;
                    SysLib.short2bytes(nextDataBlock, indirectBlock, j*2);

                    // We still need to track remaining length, keeping in mind that we've filled 11 blocks already
                    remLength = (dirI.length-(5632 + (j*512)) >= 512) ? 512 : dirI.length - (5632 + (j * 512));

                    System.arraycopy(dirAsBytes, (5632+(j*512)), buffer, 0, remLength);

                    SysLib.rawwrite(nextDataBlock, buffer);
                }

                // Finally, write out the indirect block
                SysLib.rawwrite(dirI.indirect, indirectBlock);

            }
            // If we got this far, Directory is fully initialized
            initialized = true;
            // write inode 0 back to disk
            dirI.toDisk((short)0);
        }
        catch (Exception e){
            SysLib.cerr(e.toString());
        }

        return initialized;
    }

    private synchronized boolean initializeInodes(int files){
        boolean initialized = false;
        // create an Inode that can "stamp" values onto the remaining Inode positions after 0
        Inode stamp = new Inode();
        stamp.count = 0;
        stamp.flag = Inode.UNUSED;

        try {
            // "Stamp" the default values onto every Inode# within the specified Inode blocks
            // This is slow due to reading, adjusting and writing back the block once for each Inode.
            for (short i = 1; i < superblock.totalInodes; ++i) {
                stamp.toDisk(i);
            }

            initialized = true;
        }
        catch (Exception e){
            SysLib.cerr(e.toString());
        }

        return initialized;
    }

    private synchronized boolean initializeData( int files, int diskBlocks){
        boolean initialized = false;
        byte [] buffer = new byte[512];

        // Initialize every data block but the last with the block # of the *next* block
        for(int i = 0; i < diskBlocks -1; ++i){

            // Add the next block's block number to the beginning of this block
            SysLib.short2bytes((short)(i+1), buffer, 0);
            // Write new block to disk
            SysLib.rawwrite((short)i, buffer);
        }

        // Last block (currently) has no successor
        SysLib.short2bytes((short)(-1), buffer, 0);
        SysLib.rawwrite((diskBlocks -1 ), buffer);

        return initialized;

    }

    public boolean format( int files) {
        boolean formatted = false;
        try {
            int diskBlocks = superblock.totalBlocks;

            // Create default FS components
            superblock = new Superblock(diskBlocks, files);
            directory = new Directory(files);
            filetable = new FileTable(directory, files);

            // Write new superblock to disk
            formatted &= this.initializeData( files, diskBlocks );
            formatted &= this.initializeSuperblock(superblock);
            formatted &= this.initializeDirectory(directory);
            formatted &= this.initializeInodes(files);

        }
        catch (Exception e){
            SysLib.cerr(e.toString());
        }

        return formatted;
    }

    public FileTableEntry open( String filename, String mode){
        return (FileTableEntry)null;
    }

    public boolean close( FileTableEntry ftEnt ) {
        return false;
    }

    public int fsize( FileTableEntry ftEnt ) {
        return -1;
    }

    public int read( FileTableEntry ftEnt, byte[] buffer ){
        return -1;
    }

    public int write( FileTableEntry ftEnt, byte[] buffer ){
        return -1;
    }

    private boolean deallocaAllBlocks( FileTableEntry ftEnt ){
        return false;
    }

    public boolean delete( String filename ){
        return false;
    }

    public int seek( FileTableEntry ftEnt, int offset, int whence){
        return -1;
    }




}
