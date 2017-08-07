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

    private static int getRandom(int min, int max){

        Random r = new Random();

        return r.nextInt(max - min) + min;

    }


    public void initializeBytes(byte [] block, String pattern){
        for(int i = 0; i < block.length; ++i){
            block[i] = (byte)(pattern.charAt(i % pattern.length() ));
        }
    }

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

    public boolean testReadZeroSucceeds(){
        boolean result = false;
        if(caching) {
            result = (SysLib.cread(0, new byte[512]) == OK);
        } else {
            result = (SysLib.rawread(0, new byte[512]) == OK);
        }
        return result;
    }

    public boolean testReadLastSucceeds(){
        boolean result = false;
        if(caching) {
            result = (SysLib.cread(999, new byte[512]) == OK);
        } else {
            result = (SysLib.rawread( 999, new byte[512]) == OK);
        }
        return result;
    }

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

    public boolean testReadWriteSpeeds(){
        boolean result = true;
        byte [] bytemap = new byte[512];
        int [] blocks = new int[PASSES];

        for (int i = 0; i < blocks.length; ++i){
            blocks[i] = getRandom(0,999);
        }

        long startTime = new Date().getTime();
        long endTime;

        for(int i=0; i<PASSES; ++i){
            if(caching) {
                result = result & (SysLib.cread(blocks[i], bytemap) == OK);
            }
            else{
                result = result & (SysLib.rawread(blocks[i], bytemap) == OK);

            }
        }

        endTime = new Date().getTime();
        SysLib.cout(String.format("Average read speed: %f %n", (endTime - startTime)/(1.0 * PASSES)));

        initializeBytes(bytemap, "TIMETEST");
        startTime = new Date().getTime();

        for(int i=0; i<PASSES; ++i){
            if(caching) {
                result = result & (SysLib.cwrite(blocks[i], bytemap) == OK);
            }
            else{
                result = result & (SysLib.rawwrite(blocks[i], bytemap) == OK);
            }
        }

        endTime = new Date().getTime();
        SysLib.cout(String.format("Average write speed: %f %n", (endTime - startTime)/(1.0 * PASSES)));

        return result;
    }

    // Exercise enhanced second-chance algo
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

    public Object [] testRandomAccess(){

        byte [] bytemap = new byte[512];
        byte [] readin = new byte[512];
        initializeBytes(bytemap, "DEADBEEF");

        Object [] values = new Object [3];
        boolean success = true;

        values[0] = new Date().getTime();

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

        values[1] = new Date().getTime();
        values[2] = success;

        return values;
    }

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
        values[0] = new Date().getTime();

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

        values[1] = new Date().getTime();
        values[2] = success;

        return values;
    }

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

        values[0] = new Date().getTime();

        for (int i = 0; i < PASSES; ++i){

            initializeBytes(bytemap, patterns[i%patterns.length]);

            // Similar to Localized Access, make 10 write and read access.
            // The difference is, each write/read pair has a 10% chance to access some random
            // location instead.
            for(int j = 0; j < 10; ++j){

                if(getRandom(1,10) % 10 == 0){
                    // Occasionally received ArrayIndexOutOfBoundsException from Disk.run, so let's keep the random
                    // accesses away from the edge of the disk for now.
                    actual = getRandom(1,998);
                }
                else{
                    actual = bottom + i;
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

        values[1] = new Date().getTime();
        values[2] = success;

        return values;
    }

    public Object [] testAdversarialAccess(){

        byte [] bytemap = new byte[512];
        byte [] readin = new byte[512];
        String [] patterns = {"F0F0F0F0", "123456", "ABABABAB", "ªªªªªªªª",
                "UUUUUUUU", "ðððððððð", "FEDCBA98", "77777777",
                "ÿÿÿÿÿÿÿÿ", "ÌÌÌÌÌÌÌÌ"};

        Object [] values = new Object [3];
        boolean success = true;

        // Generate a random list of tracks to access in order, so that we guarantee head movement
        // between each access.
        // It's at this point that I'd like to point how much easier this is to achieve in Python.
        List<Integer> tracks = IntStream.range(0, 10).boxed().collect(Collectors.toList());
        Collections.shuffle(tracks);

        values[0] = new Date().getTime();

        int i = 0;
        int block;

        // The random test made one random write and one random read for each test pass
        while (i < PASSES * 2){

            // Set new pattern
            initializeBytes(bytemap, patterns[i%patterns.length]);

            // Iterate repeatedly through tracks and calculate a new block address within that track to write and read.
            // This guarantees zero cache hits, so slightly worse performance than just random access
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

        values[1] = new Date().getTime();
        values[2] = success;

        return values;
    }


    /**
     * @brief   Homework 4 test runner.  Ideally, I'd use a Command Pattern here, but I'm in a hurry.
     */
    public void run(){

        // Error handling
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
            case 1: {

                SysLib.cout("SUITE 1: Random Read/Write Test \n");
                Object [] results = testRandomAccess();
                long elapsed = (long)results[1] - (long)results[0];

                SysLib.cout(String.format("All operations completed successfully: %s\n", (boolean)results[2]));

                SysLib.cout(String.format("Total runtime: %d ms\n", elapsed));

                SysLib.cout(String.format("Average time per read/write cycle: %f ms\n", elapsed/(1.0 * PASSES)));

                SysLib.flush();

                break;
            }
            case 2: {
                SysLib.cout("SUITE 2: localized writes + reads in blocks of 10 writes + 10 reads\n");
                Object [] results = testLocalizedAccess();
                long elapsed = (long)results[1] - (long)results[0];

                SysLib.cout(String.format("All operations completed successfully: %s\n", (boolean)results[2]));

                SysLib.cout(String.format("Total runtime: %d ms\n", elapsed));

                SysLib.cout(String.format("Average time per 10x write + 10x read cycle: %f ms\n", elapsed/(1.0 * PASSES)));

                SysLib.flush();

                break;
            }
            case 3: {
                SysLib.cout("SUITE 3: Mixed Access: 90% localized, 10% random\n");
                Object [] results = testMixedAccess();
                long elapsed = (long)results[1] - (long)results[0];

                SysLib.cout(String.format("All operations completed successfully: %s\n", (boolean)results[2]));

                SysLib.cout(String.format("Total runtime: %d ms\n", elapsed));

                SysLib.cout(String.format("Average time per 10x write + 10x read cycle: %f ms\n", elapsed/(1.0 * PASSES)));

                SysLib.flush();

                break;
            }
            case 4: {
                SysLib.cout("SUITE 4: Adversarial Access\n");
                Object [] results = testAdversarialAccess();
                long elapsed = (long)results[1] - (long)results[0];

                SysLib.cout(String.format("All operations completed successfully: %s\n", (boolean)results[2]));

                SysLib.cout(String.format("Total runtime: %d ms\n", elapsed));

                SysLib.cout(String.format("Average time per write/read pair: %f ms\n", elapsed/(1.0 * PASSES)));

                SysLib.flush();

                break;
            }
            // Specialized tests to validate Enhanced Second Chance algorithm
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
