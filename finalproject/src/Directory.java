import java.util.concurrent.ConcurrentHashMap;

public class Directory {
   private static int maxChars = 30; // max characters of each file name

    // Directory entries
    private int fnsizes[];        // each element stores a different file name size.
    private char fnames[][];    // each element stores a different file name.
    private ConcurrentHashMap map;

   public Directory( int maxInumber ) { // directory constructor
      fnsizes = new int[maxInumber];     // maxInumber = max files
      for ( int i = 0; i < maxInumber; i++ ) 
         fnsizes[i] = 0;                 // all file name sizes initialized to 0
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

   public short ialloc( String filename ) {
      // filename is the one of a file to be created.
      // allocates a new inode number for this filename
      return 0;
   }

   public boolean ifree( short iNumber ) {
      // deallocates this inumber (inode number)
      // the corresponding file will be deleted.
      return false;
   }

    // returns the inumber corresponding to this filename
    public short namei( String filename ) {
        short inumber;
        if(map.containsKey(filename)){
            inumber = (short)map.get(filename);
        }
        else{
            inumber = -1;
        }

        return inumber;
    }
}