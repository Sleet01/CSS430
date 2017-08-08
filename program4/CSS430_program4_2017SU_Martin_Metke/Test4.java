import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @brief   Unit and Performance tests for CSS430 Program4's Cache.java
 * @author  Martin L. Metke
 * @date    2017/07/30
 */

class Test4 extends Thread {

    // CONSTANTS
    private final static int OK = Kernel.OK;
    private final static int ERROR = Kernel.ERROR;
    private final static int PASSES = 200;

    // setting variables
    private int suite;
    private boolean caching;
    private int instError = 0;

    //////////////////////////////////////////////////////////////////////
    //                            CONSTRUCTORS                          //
    //////////////////////////////////////////////////////////////////////

    // The loader is not smart enough to handle passing a zero-length String [] to the other Constructor,
    // so this constructor handles the invocation "l Test4".
    public Test4(){
        this.instError = 1;
    }

    /**
     * @brief   The constructor that testing should always use
     * @pre     N/A
     * @post    A new Test4 thread will be created.  The suite to run, the caching
     *          mode (caching = "true" or "false"), and any runtime errors will be recorded
     *          in private instance variables.
     * @param argv      String[] containing the caching option and test suite to use
     *                  argv[0]:    "enabled": use cache
     *                              "disabled": use raw writes/reads to/from "disk"
     *                  argv[1]:    "1" - "4" = Program4 required perf tests
     *                              "1":    Fully randomized writes and reads
     *                              "2":    Fully localized writes and reads to 10 contiguous blocks
     *                              "3":    Mixed localized/random access; 90%/10%
     *                              "4":    "Adversarial" access: every read or write is to a different "track"
     *                              "0":    Development unit tests of various Cache.java methods
     *                              "5":    Specific test of cache and second-chance algorithm
     *                                      (requires instrumentation in Cache.java to be uncommented)
     */
    public Test4(String [] argv){

        // Only accepts 2 arguments
        if(argv.length != 2){
            this.instError = 1;
            return;
        }

        // Enabled or disable caching (MUST EXPLICITLY BE SET)
        if(argv[0].toLowerCase().equals( "enabled" )){
            this.caching = true;
        }
        else if (argv[0].toLowerCase().equals( "disabled" )){
            this.caching = false;
        }
        else {
            this.instError = 2;
            return;
        }

        // Get "suite" of tests to run
        this.suite = Integer.parseInt(argv[1]);
        // Suites 0 and 5 are development-time unit tests
        if(!(this.suite >= 0 && this.suite <= 5)){
            this.instError = 3;
        }

    }


    //////////////////////////////////////////////////////////////////////
    //                         UNIT TEST METHODS                        //
    //////////////////////////////////////////////////////////////////////

    // Generate a random integer between min and max, inclusive
    private static int getRandom(int min, int max){

        Random r = new Random();

        return r.nextInt(max - min) + min;

    }

    // Fill a given byte [] with a repeated pattern of "char" bytes taken from a String
    public void initializeBytes(byte [] block, String pattern){
        for(int i = 0; i < block.length; ++i){
            block[i] = (byte)(pattern.charAt(i % pattern.length() ));
        }
    }

    // Unit test that reads out of bounds fail and return "false"
    // Note: rather than returning ERROR, it appears that one of Disk, Kernel, or SysLib
    // throws an exception and hangs when an OOB access is attempted, so this test
    // automatically fails when caching is disabled.
    public boolean testBadReadFails(){
        boolean result = false;
        if(caching) {
            result = (SysLib.cread(-1, new byte[512]) == ERROR);
            result = result & (SysLib.cread(1000, new byte[512]) == ERROR);
        } else {
            // This automatically fails with rawreads.
            result = false;
        }
        return result;
    }

    // Unit test that checks if a read to disk block 0 succeeds
    public boolean testReadZeroSucceeds(){
        boolean result = false;
        if(caching) {
            result = (SysLib.cread(0, new byte[512]) == OK);
        } else {
            result = (SysLib.rawread(0, new byte[512]) == OK);
        }
        return result;
    }

    // Unit test that checks if a read to disk block 999 succeeds
    public boolean testReadLastSucceeds(){
        boolean result = false;
        if(caching) {
            result = (SysLib.cread(999, new byte[512]) == OK);
        } else {
            result = (SysLib.rawread( 999, new byte[512]) == OK);
        }
        return result;
    }

    // Unit test of write to disk block 0
    public boolean testWriteToZero(){
        boolean result = false;
        boolean wrote = false;
        boolean read = false;
        byte [] bytemap = new byte[512];
        byte [] readback = new byte [512];
        initializeBytes(bytemap, "01010101" );

        if(caching){
            wrote = ( SysLib.cwrite(0, bytemap) == OK);
            read = ( SysLib.cread(0, readback) == OK);
        } else {
            wrote = ( SysLib.rawwrite(0, bytemap) == OK);
            read = ( SysLib.rawread(0, readback) == OK);
        }

        if(wrote && read){
            result = Arrays.equals(bytemap, readback);
        } else {
            result = false;
        }

        return result;
    }

    // Unit test of write to disk block 999
    public boolean testWriteToLast(){
        boolean result = false;
        boolean wrote = false;
        boolean read = false;
        byte [] bytemap = new byte[512];
        byte [] readback = new byte [512];
        initializeBytes(bytemap, "10101010" );

        if(caching){
            wrote = ( SysLib.cwrite(999, bytemap) == OK);
            read = ( SysLib.cread(999, readback) == OK);
        } else {
            wrote = ( SysLib.rawwrite(999, bytemap) == OK);
            read = ( SysLib.rawread(999, readback) == OK);
        }

        if(wrote && read){
            result = Arrays.equals(bytemap, readback);
        } else {
            result = false;
        }

        return result;
    }

    // Compares read and write speeds to verify that write-back and read-in are working properly
    public boolean testReadWriteSpeeds(){
        boolean result = true;
        byte [] bytemap = new byte[512];
        int [] blocks = new int[PASSES];

        for (int i = 0; i < blocks.length; ++i){
            blocks[i] = getRandom(0,999);
        }

        long startTime = System.nanoTime();
        long endTime;

        for(int i=0; i<PASSES; ++i){
            if(caching) {
                result = result & (SysLib.cread(blocks[i], bytemap) == OK);
            }
            else{
                result = result & (SysLib.rawread(blocks[i], bytemap) == OK);

            }
        }

        endTime = System.nanoTime();
        SysLib.cout(String.format("Average read speed: %f %n", (endTime - startTime)/(1000000.0 * PASSES)));

        initializeBytes(bytemap, "TIMETEST");
        startTime = System.nanoTime();

        for(int i=0; i<PASSES; ++i){
            if(caching) {
                result = result & (SysLib.cwrite(blocks[i], bytemap) == OK);
            }
            else{
                result = result & (SysLib.rawwrite(blocks[i], bytemap) == OK);
            }
        }

        endTime = System.nanoTime();
        SysLib.cout(String.format("Average write speed: %f %n", (endTime - startTime)/(1000000.0 * PASSES)));

        return result;
    }

    // Exercise enhanced second-chance algorithm by filling, invalidating, and re-filling
    // blocks in the buffer.
    public boolean testSecondChance(){
        boolean result = true;
        byte [] bytemap = new byte[512];

        // Fill cache
        for(int i = 0; i<10; ++i){
            result = result & (SysLib.cread(i, bytemap) == OK);
        }
        // Invalidate the first block by reading in block 11
        result = result & (SysLib.cread(10, bytemap) == OK);

        // re-read from 1-9
        for(int i = 1; i<10; ++i){
            result = result & (SysLib.cread(i, bytemap) == OK);
        }

        // Invalidate the first block by reading in block 0
        result = result & (SysLib.cread(0, bytemap) == OK);

        // re-read from 0-9
        for(int i = 0; i<10; ++i){
            result = result & (SysLib.cread(i, bytemap) == OK);
        }

        // Cache state should now be:
        // blocks 0-9 are in state 0b10 (referenced, not dirty)

        SysLib.flush();
        initializeBytes(bytemap, "TEST ENHANCED SECOND-CHANCE ALGO");

        // Fill cache
        for(int i = 0; i<10; ++i){
            result = result & (SysLib.cwrite(i, bytemap) == OK);
        }

        // re-read from 0-9
        for(int i = 0; i<10; ++i){
            result = result & (SysLib.cread(i, bytemap) == OK);
        }

        // Cache state should now be:
        // Blocks 0-9 are in state 0b11 (referenced, dirty)

        // Invalidate the first block by reading in block 10
        result = result & (SysLib.cread(10, bytemap) == OK);

        return result;
    }


    //////////////////////////////////////////////////////////////////////
    //                         MAIN TEST METHODS                        //
    //////////////////////////////////////////////////////////////////////

    /**
     * @brief   Test of cached or uncached random writes and reads.
     * @pre     Kernel has initialized a Cache.  Cache is new or has been flushed.
     * @post    Pattern "DEADBEEF" will be written to 200 blocks on disk.  Cache will be full.
     * @return  values      Object[] containing test results
     *                      values[0]:  start time of the test
     *                      values[1]:  end time of the test
     *                      values[2]:  boolean indicating whether all operations succeeded or not
     */
    public Object [] testRandomAccess(){

        byte [] bytemap = new byte[512];
        byte [] readin = new byte[512];
        initializeBytes(bytemap, "DEADBEEF");

        Object [] values = new Object [3];
        boolean success = true;

        values[0] = System.nanoTime();

        if(caching) {
            for (int i = 0; i < PASSES; ++i) {
                success = success & (SysLib.cwrite(getRandom(0, 999), bytemap) == OK);
                success = success & (SysLib.cread(getRandom(0, 999), readin) == OK);

            }
        } else {
            for (int i = 0; i < PASSES; ++i) {
                success = success & (SysLib.rawwrite(getRandom(0, 999), bytemap) == OK);
                success = success & (SysLib.rawread(getRandom(0, 999), readin) == OK);
            }
        }

        values[1] = System.nanoTime();
        values[2] = success;

        return values;
    }

    /**
     * @brief   Test of completely localized access (best-case scenario for caching) either cached or uncached.
     * @pre     Kernel has initialized a Cache.  Cache is new or has been flushed.
     * @post    10 rotating patterns will be written on disk in a 10-block segment.  Cache will be full.
     * @return  values      Object[] containing test results
     *                      values[0]:  start time of the test
     *                      values[1]:  end time of the test
     *                      values[2]:  boolean indicating whether all operations succeeded or not
     */
    public Object [] testLocalizedAccess(){

        byte [] bytemap = new byte[512];
        byte [] readin = new byte[512];
        String [] patterns = {"a0a0a0a0", "b1b1b1b1", "c2c2c2c2", "d3d3d3d3",
                "e4e4e4e4", "f5f5f5f5", "g6g6g6g6", "h7h7h7h7",
                "i8i8i8i8", "j9j9j9j9"};

        // Set a new span of disk blocks to access.
        int bottom = 10 * getRandom(0,98);
        int top = bottom + 10;

        Object [] values = new Object [3];
        boolean success = true;

        // Initial start time
        values[0] = System.nanoTime();

        // Make PASSES x passes x 10 writes and 10 reads (with check)
        for (int k = 0; k < PASSES; ++k){

            // Get a new pattern to write and read back
            initializeBytes(bytemap, patterns[k % patterns.length]);

            if (caching) {
                // Initialize 10 locations (fills the cache)
                for (int i = 0; i < 10; ++i) {
                    success = success & (SysLib.cwrite(bottom + i, bytemap) == OK);
                }
                // Read back written blocks and check for correctness
                for (int i = 0; i < 10; ++i) {
                    success = success & (SysLib.cread(bottom + i, readin) == OK);
                    success = success & (Arrays.equals(bytemap, readin));
                }
            } else {
                // Initialize 10 locations (fills the blocks on disk)
                for (int i = 0; i < 10; ++i) {
                    success = success & (SysLib.rawwrite(bottom + i, bytemap) == OK);
                }
                // Spend some time reading back random blocks within the specified locality
                for (int i = 0; i < 10; ++i) {
                    success = success & (SysLib.rawread(bottom + i, readin) == OK);
                    success = success & (Arrays.equals(bytemap, readin));
                }
            }
        }

        values[1] = System.nanoTime();
        values[2] = success;

        return values;
    }

    /**
     * @brief   Test of mixed (90% localized, 10% random) access, either cached or uncached.
     * @pre     Kernel has initialized a Cache.  Cache is new or has been flushed.
     * @post    10 rotating patterns will be written on disk with 90% of blocks being contiguous
     *          and 10% being randomly selected from the full range of the disk.
     * @return  values      Object[] containing test results
     *                      values[0]:  start time of the test
     *                      values[1]:  end time of the test
     *                      values[2]:  boolean indicating whether all operations succeeded or not
     */
    public Object [] testMixedAccess(){

        byte [] bytemap = new byte[512];
        byte [] readin = new byte[512];
        String [] patterns = {"a0a0a0a0", "b1b1b1b1", "c2c2c2c2", "d3d3d3d3",
                "e4e4e4e4", "f5f5f5f5", "g6g6g6g6", "h7h7h7h7",
                "i8i8i8i8", "j9j9j9j9"};

        Object [] values = new Object [3];
        boolean success = true;

        int bottom = 10 * getRandom(0,99);
        int actual;

        values[0] = System.nanoTime();

        for (int i = 0; i < PASSES; ++i){

            initializeBytes(bytemap, patterns[i%patterns.length]);

            // Similar to Localized Access, make 10 write and read access.
            // The difference is, each write/read pair has a 10% chance to access some random
            // location instead.
            for(int j = 0; j < 10; ++j){

                if(getRandom(1,10) % 10 == 0){
                    // Occasionally received ArrayIndexOutOfBoundsException from Disk.run, so let's keep the random
                    // accesses away from the edge of the disk for now.
                    actual = getRandom(1,999);
                }
                else{
                    actual = bottom + j;
                }

                if (caching) {
                    // Initialize location
                    success = success & (SysLib.cwrite(actual, bytemap) == OK);
                    // Read back written blocks and check for correctness
                    success = success & (SysLib.cread(actual, readin) == OK);
                    success = success & (Arrays.equals(bytemap, readin));
                } else {
                    // Initialize location
                    success = success & (SysLib.rawwrite(actual, bytemap) == OK);
                    // Spend some time reading back random blocks within the specified locality
                    success = success & (SysLib.rawread(actual, readin) == OK);
                    success = success & (Arrays.equals(bytemap, readin));
                }
            }
        }

        values[1] = System.nanoTime();
        values[2] = success;

        return values;
    }

    /**
     * @brief   Test of adversarial access.  Every
     * @pre     Kernel has initialized a Cache.  Cache is new or has been flushed.
     * @post    10 rotating patterns will be written on disk with 90% of blocks being contiguous
     *          and 10% being randomly selected from the full range of the disk.
     * @return  values      Object[] containing test results
     *                      values[0]:  start time of the test
     *                      values[1]:  end time of the test
     *                      values[2]:  boolean indicating whether all operations succeeded or not
     */
    public Object [] testAdversarialAccess(){

        byte [] bytemap = new byte[512];
        byte [] readin = new byte[512];
        String [] patterns = {"F0F0F0F0", "123456", "ABABABAB", "ªªªªªªªª",
                "UUUUUUUU", "ðððððððð", "FEDCBA98", "77777777",
                "ÿÿÿÿÿÿÿÿ", "ÌÌÌÌÌÌÌÌ"};

        Object [] values = new Object [3];
        boolean success = true;

        // Generate a random list of unique tracks to access in order, so that we guarantee
        // head movement between each access.
        // It's at this point that I'd like to point how much easier this is to achieve in Python.
        List<Integer> tracks = IntStream.range(0, 10).boxed().collect(Collectors.toList());
        Collections.shuffle(tracks);

        values[0] = System.nanoTime();

        int i = 0;
        int block;

        // The random test made one random write and one random read for each test pass so
        // I doubled up the number of passes, but each one is *either* a read or a write.
        // This should make comparisons between the Random and Adversarial tests simpler.
        while (i < PASSES * 2){

            // Set new pattern
            initializeBytes(bytemap, patterns[i%patterns.length]);

            // Iterate repeatedly through tracks and calculate a new block address within that track to write or read.
            // This guarantees zero cache hits, so equal or slightly worse performance than just random access.
            block = (tracks.get(i % tracks.size()) * 100) + getRandom(0,99);

            if(caching){
                if(i % 2 == 0){
                    // Write to a location
                    success = success & (SysLib.cwrite(block, bytemap) == OK);
                }
                else{
                    // Read from a location
                    success = success & (SysLib.cread(block, readin) == OK);
                }
            }
            else{
                if(i % 2 == 0){
                    // Write to a location
                    success = success & (SysLib.rawwrite(block, bytemap) == OK);
                }
                else{
                    // Read from a location
                    success = success & (SysLib.rawread(block, readin) == OK);
                }
            }

            ++i;
        }

        values[1] = System.nanoTime();
        values[2] = success;

        return values;
    }


    /**
     * @brief   Homework 4 test runner.  Ideally, I'd use a Command Pattern here, but I'm in a hurry.
     * @pre     Kernel has a Cache instance.  Cache instance is empty or has been flushed.
     * @post    Cache will be empty, as all operations call "flush" before exiting.
     */
    public void run(){

        // Error handling; it appears that ThreadOS has issues if you call exit() from within a constructor
        // so we bank the instantiation-time errors until this thread begins running, and then immediately exit out.
        switch(this.instError) {
            case 0: break;
            case 1: {
                SysLib.cout("Incorrect Test4 invocation!  Use \"Test4 <enabled|disabled> <0-4>\"" + "\n");
                SysLib.exit();
                break;
            }
            case 2: {
                SysLib.cout("Invalid caching selection!  Use \"enabled\" or \"disabled\"" + "\n");
                SysLib.exit();
                break;
            }
            case 3: {
                SysLib.cout("Invalid test suite selection!  Use \"0\" through \"4\"" + "\n");
                SysLib.exit();
                break;
            }
        }

        // Choose which test suite to run based on the second parameter passed to the constructor
        switch(this.suite) {
            // Unit Tests, not for "production".
            case 0: {
                int count = 0;

                // Start of tests.  First test initializes "last" variable, which we'll use from here on out.
                boolean last = testBadReadFails();

                SysLib.cout("Test 1: OOB read calls fail: " + ((last) ? "PASS" : "FAIL") + "\n");
                count += ((last) ? 0 : 1);

                last = testReadZeroSucceeds();
                SysLib.cout("Test 2: Read from Block 0 succeeds: " + ((last) ? "PASS" : "FAIL") + "\n");
                count += ((last) ? 0 : 1);

                last = testReadLastSucceeds();
                SysLib.cout("Test 3: Read from Block 999 succeeds: " + ((last) ? "PASS" : "FAIL") + "\n");
                count += ((last) ? 0 : 1);

                last = testWriteToZero();
                SysLib.cout("Test 4: Write to Block 0 succeeds: " + ((last) ? "PASS" : "FAIL") + "\n");
                count += ((last) ? 0 : 1);

                last = testWriteToLast();
                SysLib.cout("Test 5: Write to Block 999 succeeds: " + ((last) ? "PASS" : "FAIL") + "\n");
                count += ((last) ? 0 : 1);

                last = testReadWriteSpeeds();
                SysLib.cout("Test 6: read and write 200 random blocks: " + ((last) ? "PASS" : "FAIL") + "\n");
                count += ((last) ? 0 : 1);

                // Output totals
                SysLib.cout("TOTAL FAILURES: " + count + "\n");

                SysLib.flush();

                break;

            }
            // Random access
            case 1: {

                SysLib.cout("SUITE 1: Random Read/Write Test \n");
                Object [] results = testRandomAccess();
                long elapsed = ((long)results[1] - (long)results[0])/1000000;

                SysLib.cout(String.format("All operations completed successfully: %s\n", (boolean)results[2]));

                SysLib.cout(String.format("Total runtime: %d ms\n", elapsed));

                SysLib.cout(String.format("Average time per read/write cycle: %f ms\n", elapsed/(1.0 * PASSES)));

                SysLib.flush();

                break;
            }
            // Fully localized (10 contiguous blocks) access
            case 2: {
                SysLib.cout("SUITE 2: localized writes + reads in blocks of 10 writes + 10 reads\n");
                Object [] results = testLocalizedAccess();
                long elapsed = ((long)results[1] - (long)results[0])/1000000;

                SysLib.cout(String.format("All operations completed successfully: %s\n", (boolean)results[2]));

                SysLib.cout(String.format("Total runtime: %d ms\n", elapsed));

                SysLib.cout(String.format("Average time per 10x write + 10x read cycle: %f ms\n", elapsed/(1.0 * PASSES)));

                SysLib.flush();

                break;
            }
            // Mixed access, with 90% of access localized and 10% randomized
            case 3: {
                SysLib.cout("SUITE 3: Mixed Access: 90% localized, 10% random\n");
                Object [] results = testMixedAccess();
                long elapsed = ((long)results[1] - (long)results[0])/1000000;

                SysLib.cout(String.format("All operations completed successfully: %s\n", (boolean)results[2]));

                SysLib.cout(String.format("Total runtime: %d ms\n", elapsed));

                SysLib.cout(String.format("Average time per 10x write + 10x read cycle: %f ms\n", elapsed/(1.0 * PASSES)));

                SysLib.flush();

                break;
            }
            // Adversarial access: each successive access hits a different block on a different randomly-selected
            // *track*, to (nominally) exercise head movement in the Disk class.
            case 4: {
                SysLib.cout("SUITE 4: Adversarial Access\n");
                Object [] results = testAdversarialAccess();
                long elapsed = ((long)results[1] - (long)results[0])/1000000;

                SysLib.cout(String.format("All operations completed successfully: %s\n", (boolean)results[2]));

                SysLib.cout(String.format("Total runtime: %d ms\n", elapsed));

                SysLib.cout(String.format("Average time per write/read pair: %f ms\n", elapsed/(1.0 * PASSES)));

                SysLib.flush();

                break;
            }
            // Specialized tests to validate Enhanced Second Chance algorithm; requires uncommenting some
            // SysLib.cerr lines in Cache.java to utilize.
            case 5: {
                int count = 0;

                // Start of tests.  First test initializes "last" variable, which we'll use from here on out.
                boolean last = testSecondChance();

                SysLib.cout("Test 5.1: Exercise Enhanced Second-Chance algorithm: " + ((last) ? "PASS" : "FAIL") + "\n");
                count += ((last) ? 0 : 1);

                // Output totals
                SysLib.cout("TOTAL FAILURES: " + count + "\n");

                SysLib.flush();

                break;
            }
        }

        // Exit cleanly
        SysLib.exit();
    }
}
