import java.util.ArrayList;
import java.util.List;

public class Inode {
    private final static int iNodeSize = 32;       // fix to 32 bytes
    private final static int directSize = 11;      // # direct pointers
    private final static int[] fieldSizes = {4, 2, 2,
                                             2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
                                             2};    // Field sizes of a serialized Inode
    private final static int[] indirectSizes = {
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2
    }; // This is the field size array for the indirect block, which is 256 shorts in 512 bytes

    // Flag values
    public final static short UNUSED =    0;
    public final static short USED =      1;
    public final static short DELETE =    2;

    public int length;                             // file size in bytes
    public short count;                            // # file-table entries pointing to this
    public short flag;                             // 0 = unused, 1 = used, ...
    public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // a indirect pointer

    public Inode( ) {                                     // a default constructor
        length = 0;
        count = 0;
        flag = USED;
        for ( int i = 0; i < directSize; i++ )
            direct[i] = -1;
        indirect = -1;
    }

    public Inode( short iNumber ) {                       // retrieving inode from disk
        short block = (short)(iNumber / 16); // block 0 if iNumber is 0-15, 1 if 16-31, etc.
        int offset = iNumber % 16;           // and the remainder shows the offset within the block

        // Prep to load
        List<Object> fields = SysLib.disk2List(block, offset, fieldSizes);

        // Try to read in fields
        if(fields.get(0) != null) { // read-in from disk has succeeded
            this.length = (int) fields.get(0);
            this.count = (short) fields.get(1);
            this.flag = (short) fields.get(2);
            for (int i = 0; i < directSize; ++i){
                this.direct[i] = (short) fields.get(3 + i);
            }
            this.indirect = (short) fields.get(fields.size() -1);
        }
        else{ // Something has gone wrong; initialize with default values
            length = 0;
            count = 0;
            flag = 1;
            for ( int i = 0; i < directSize; i++ )
                direct[i] = -1;
            indirect = -1;
        }
    }

    @Override
    public boolean equals(Object other){

        // If identical objects, equal
        if (other == this){
            return true;
        }

        // If other is not an Inode, cannot be equal
        if (!(other instanceof Inode)){
            return false;
        }

        // Cast other to Inode for comparison
        Inode o = (Inode)other;

        // Assume true; "and" equality checks over all relevant fields
        // Note: count and flag are not guaranteed to be identical, but if all
        // fields are otherwise equal, o and this should be identical
        boolean equal = true;
        equal = equal & (o.length == this.length);
        equal = equal & (o.indirect == this.indirect);
        for (int i = 0; i < 11; ++i){
            equal = equal & (o.direct[i] == this.direct[i]);
        }
        return equal;
    }

    public int toDisk( short iNumber ) {                  // save to disk as the i-th inode
        short block = (short)(iNumber / 16); // block 0 if iNumber is 0-15, 1 if 16-31, etc.
        int offset = iNumber % 16;           // and the remainder shows the offset within the block

        List<Object> fields = new ArrayList<Object>(fieldSizes.length);

        // Marshal fields into the list
        fields.set(0, this.length);
        fields.set(1, this.count);
        fields.set(2, this.flag);
        for (int i = 0; i < directSize; ++i){
            fields.set(3 + i, this.direct[i]);
        }
        fields.set(fields.size() -1, this.indirect);

        // request write-back; this should only overwrite the bytes specific to this Inode
        return SysLib.list2Disk(fields, fieldSizes, block, offset);
    }
}