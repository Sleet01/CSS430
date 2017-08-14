import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Directory {
    private static int maxChars = 30; // max characters of each file name
    private int freeInodes;     // Track how many inodes remain
    private short nextFreeInode;  // Keep track of the next free Inode

    // Directory entries
    private int fnsizes[];        // each element stores a different file name size.
    private char fnames[][];    // each element stores a different file name.
    private ConcurrentHashMap map;

    public Directory( int maxInumber ) { // directory constructor

        freeInodes = maxInumber - 1;        // Remaining free inodes, after the directory's is used
        nextFreeInode = 1;                  // Track the next available Inode

        fnsizes = new int[maxInumber];     // maxInumber = max files
        for ( int i = 0; i < maxInumber; i++ ) {
            fnsizes[i] = 0;                 // all file name sizes initialized to 0
        }

        fnames = new char[maxInumber][maxChars];
        String root = "/";                // entry(inode) 0 is "/"
        fnsizes[0] = root.length( );        // fnsizes[0] is the size of "/".
        root.getChars( 0, fnsizes[0], fnames[0], 0 ); // fnames[0] includes "/"

        // Set mapping for root "/" value
        map = new ConcurrentHashMap(maxInumber);
        map.put(root, (short)0);
    }

   public int bytes2directory( byte data[] ) {
      // assumes data[] received directory information from disk
      // initializes the Directory instance with this data[]
       return -1;
   }

   public byte[] directory2bytes( ) {
      // converts and return Directory information into a plain byte array
      // this byte array will be written back to disk
      // note: only meaningfull directory information should be converted
      // into bytes.
      return null;
   }

    public synchronized short ialloc( String filename ) {
        // filename is the one of a file to be created.
        // allocates a new inode number for this filename
        short inumber = -1;

        // Can only assign a new Inode if any are available
        if(!(nextFreeInode == -1 || freeInodes == 0)) {

            // Only allow a new Inode allocation if the file doesn't already exist
            if (!map.containsKey(filename)) {
                inumber = nextFreeInode;
                --freeInodes;
                nextFreeInode = findNextFreeInode(nextFreeInode);

                // register new filename in fnsize, fnames, and map
                fnsizes[inumber] = filename.length();
                filename.getChars(0, fnsizes[inumber], fnames[inumber], 0);
                map.put(filename, (short)inumber );
            }
        }

        return inumber;
    }

    private short findNextFreeInode(short lastFree){
        short next = -1;
        short state;
        short offset = (short)(((lastFree + 1) * 32) % 512 ); // find next Inode's offset w/in block
        short iblock = (short)(lastFree/16);                  // find next block to look in

        if(freeInodes > 0){
            byte[] buffer = new byte[512];
            SysLib.rawread(iblock, buffer);

            // Loop through end of Inode-containing blocks to find the next unused block
            short i = (short)(lastFree + 1);
            while (i < fnames.length){

                state = SysLib.bytes2short(buffer, offset + 6);
                if(state == Inode.UNUSED){
                    next = i;
                    break;
                }
                else{
                    ++i;
                    offset = (short)(i * 32);
                    if(offset >= 512){
                        SysLib.rawread(++iblock, buffer);
                        offset %= 512;      // Ensure offset is within one block
                    }
                }
            }
        }
        return next;
    }

   public synchronized boolean ifree( short iNumber ) {
      // deallocates this inumber (inode number)
      // the corresponding file will be deleted.
      return false;
   }

    // returns the inumber corresponding to this filename
    public short namei( String filename ) {
        short inumber;

        // All filenames should be contained in the map, including the root "/" dir.
        if(map.containsKey(filename)){
            inumber = (short)map.get(filename);
        }
        else{
            inumber = -1;
        }

        return inumber;
    }
}