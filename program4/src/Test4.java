import java.util.Arrays;
import java.util.Date;
import java.util.Random;


class Test4 extends Thread {

    // CONSTANTS
    private final static int OK = Kernel.OK;
    private final static int ERROR = Kernel.ERROR;
    private final static int PASSES = 200;

    // setting variables
    private int suite;
    private boolean caching;
    private int instError = 0;

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
        if(!(this.suite >= 0 && this.suite <= 4)){
            this.instError = 3;
        }

    }


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

        Object [] values = new Object [3];
        boolean success = true;

        int bottom = getRandom(0,989);
        int top = bottom + 10;

        values[0] = new Date().getTime();

        for (int j = 0; j < patterns.length; ++j) {

            initializeBytes(bytemap, patterns[j]);

            if (caching) {
                // Initialize 10 locations (fills the cache)
                for (int i = 0; i < 10; ++i) {
                    success = success & (SysLib.cwrite(bottom + i, bytemap) == OK);
                }
                for (int i = 0; i < PASSES/10; ++i) {
                    success = success & (SysLib.cread(getRandom(bottom, top), readin) == OK);
                    success = success & (Arrays.equals(bytemap, readin));
                }
            } else {
                for (int i = 0; i < 10; ++i) {
                    success = success & (SysLib.rawwrite(bottom + i, bytemap) == OK);
                }
                for (int i = 0; i < PASSES/10; ++i) {
                    success = success & (SysLib.rawread(getRandom(bottom, top), readin) == OK);
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

        int bottom = getRandom(0,989);
        int top = bottom + 10;

        values[0] = new Date().getTime();

        for (int j = 0; j < patterns.length; ++j) {

            initializeBytes(bytemap, patterns[j]);

            if (caching) {
                // Initialize 10 locations (fills the cache)
                for (int i = 0; i < 10; ++i) {
                    success = success & (SysLib.cwrite(bottom + i, bytemap) == OK);
                }
                for (int i = 0; i < PASSES; ++i) {
                    success = success & (SysLib.cread(getRandom(bottom, top), readin) == OK);
                    success = success & (Arrays.equals(bytemap, readin));
                }
            } else {
                for (int i = 0; i < 10; ++i) {
                    success = success & (SysLib.rawwrite(bottom + i, bytemap) == OK);
                }
                for (int i = 0; i < PASSES; ++i) {
                    success = success & (SysLib.rawread(getRandom(bottom, top), readin) == OK);
                    success = success & (Arrays.equals(bytemap, readin));
                }
            }
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
                SysLib.cout("Invalid suite selection!  Use \"0\" through \"4\"" + "\n");
                SysLib.exit();
                break;
            }
        }

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
                SysLib.cout("SUITE 2: localized writes + reads\n");
                Object [] results = testLocalizedAccess();
                long elapsed = (long)results[1] - (long)results[0];

                SysLib.cout(String.format("All operations completed successfully: %s\n", (boolean)results[2]));

                SysLib.cout(String.format("Total runtime: %d ms\n", elapsed));

                SysLib.cout(String.format("Average time per read/write cycle: %f ms\n", elapsed/(1.0 * PASSES)));

                SysLib.flush();

                break;
            }
            case 3: {
                SysLib.cout("This suite is not yet implemented!" + "\n");
                break;
            }
            case 4: {
                SysLib.cout("This suite is not yet implemented!" + "\n");
                break;
            }
        }

        // Exit cleanly
        SysLib.exit();
    }
}
