import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class FileTable {

    private Vector<FileTableEntry> table;         // the actual entity of this file table
    private Directory dir;        // the root directory
    private List<Inode> inodes;   // Track all used Inodes, for updating purposes


    public FileTable( Directory directory, int maxInodes ) { // constructor
        table = new Vector<FileTableEntry>( );     // instantiate a file (structure) table
        dir = directory;           // receive a reference to the Directory
                                   // from the file system
        inodes = new ArrayList<Inode>(maxInodes);   // ArrayList of used inodes
    }

    // major public methods
    public synchronized FileTableEntry falloc( String filename, String mode ) {
        // allocate a new file (structure) table entry for this file name
        // allocate/retrieve and register the corresponding inode using dir
        // increment this inode's count
        // immediately write back this inode to the disk
        // return a reference to this file (structure) table entry
        FileTableEntry newFTE;
        Inode inode;
        short inumber = dir.namei(filename);

        // Case 1 or 2: file exists on disk
        if(inumber != -1){

            // Create a new Inode based on this inumber, since we know the file should exist
            // If there is no existing Inode in the inodes list, no other process is accesssing it
            inode = new Inode(inumber);

            // Check if this Inode is already in use
            for (Inode i: inodes) {
                if (i.equals(inode)) {
                    inode = i;
                    ++inode.count;
                    break;
                }
            }
            // Now we have an Inode instance and an inumber
        } else {

            // Case 3: file does not exist on disk, nobody is accessing it
            inumber = dir.ialloc(filename);
            inode = new Inode();

            // Now we have an Inode instance and an inumber
        }

        // Create newFTE with inode and inumber we have gotten
        newFTE = new FileTableEntry(inode, inumber, mode);

        // Record new FTE and, if necessary, the inode
        table.add(newFTE);
        if(!inodes.contains(inode)){
            inodes.add(inode);
        }
        // Write back inode
        inode.toDisk(inumber);

        return newFTE;
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

//               // If inode's count has reach 0 or below, mark it as unused
//               if(inode.count <= 0){
//                   inode.count = 0;
//                   inode.flag = (inode.flag == inode.DELETE) ? inode.DELETE : inode.UNUSED;
//               }

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