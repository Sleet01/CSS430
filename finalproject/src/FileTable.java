import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class FileTable {

    private Vector table;         // the actual entity of this file table
    private Directory dir;        // the root directory
    private List inodes;   // Track all used Inodes, for updating purposes
    private int nextFreeInode;  // Keep track of the next free Inode

    public FileTable( Directory directory, int maxInodes ) { // constructor
        table = new Vector( );     // instantiate a file (structure) table
        dir = directory;           // receive a reference to the Director
                                   // from the file system
        nextFreeInode = 1;          // directory always get Inode 0
        inodes = new ArrayList<Inode>(maxInodes);   // ArrayList of inodes

    }

    // major public methods
    public synchronized FileTableEntry falloc( String filename, String mode ) {
        // allocate a new file (structure) table entry for this file name
        // allocate/retrieve and register the corresponding inode using dir
        // increment this inode's count
        // immediately write back this inode to the disk
        // return a reference to this file (structure) table entry
        Inode inode;

        // Case 1: file exists on disk, somebody is accessing it
        if(dir.namei(filename) != -1){

        }

        // Case 2: file exists on disk, but nobody is accessing it

        // Case 3: file does not exist on disk, nobody is accessing it

         return null;
    }

   public synchronized boolean ffree( FileTableEntry e ) {
      // receive a file table entry reference
      // save the corresponding inode to the disk
      // free this file table entry.
      // return true if this file table entry found in my table
       boolean found = false;

       // Check if e is in table; if so, unlink (decrement count) from the Inode,
       // Save back the Inode to disk (using the FTE's iNumber entry).
       if(table.contains(e)){
           Inode inode = e.inode;

           // Attempt Inode write-back
           if((inode.toDisk(e.iNumber)) != Kernel.ERROR){
               // decrement the count of FTE references on the Inode
               --inode.count;

               // If inode's count has reach 0 or below, mark it as unused
               if(inode.count <= 0){
                   inode.count = 0;
                   inode.flag = (inode.flag == inode.DELETE) ? inode.DELETE : inode.UNUSED;
               }

               // Remove e from the table of used FTEs
               table.remove(e);

               found = true;
           }
       }
      return found;
   }

   public synchronized boolean fempty( ) {
      return table.isEmpty( );  // return if table is empty 
   }                            // should be called before starting a format
}