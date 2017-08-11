public class FileSystem {
    private Superblock superblock;
    private Directory directory;
    private FileTable filetable;

    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    public FileSystem( int diskBlocks ){
        // default superblock creation and formatting
        superblock = new Superblock( diskBlocks );

        // create new Directory and set root "/" as first entry
        directory = new Directory( superblock.totalInodes );

        // Link directory to new FileTable
        filetable = new FileTable( directory );

        // Recreate the root entry if one exists
        FileTableEntry dirEnt = open( "/", "r");
        int dirSize = fsize( dirEnt );
        if ( dirSize > 0 ) {
            byte[] dirData = new byte[dirSize];
            read(dirEnt, dirData);
            directory.bytes2directory(dirData);
        }
        close( dirEnt );
    }

    public void sync( ) {

    }

    public boolean format( int files) {
        return false;
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
