import java.util.Date;



class Test4 extends Thread {

    public final static int OK = Kernel.OK;
    public final static int ERROR = Kernel.ERROR;


    public boolean testBadReadFails(){
        boolean result = false;
        result = ( SysLib.cread(-1, new byte[512]) == ERROR);
        result = result & ( SysLib.cread( 1000, new byte[512]) == ERROR);
        return result;
    }

    public boolean testReadZeroSucceeds(){
        boolean result = false;
        result = ( SysLib.cread(0, new byte[512]) == OK);
        return result;
    }

    public boolean testReadLastSucceeds(){
        boolean result = false;
        result = ( SysLib.cread(999, new byte[512]) == OK);
        return result;
    }

    /**
     * @brief   Homework 4 test runner.  Ideally, I'd use a Command Pattern here, but I'm in a hurry.
     */
    public void run(){

        int count = 0;

        // Start of tests.  First test initializes "last" variable, which we'll use from here on out.
        boolean last = testBadReadFails();

        SysLib.cout("Test 1: OOB read calls fail: " + ((last) ? "PASS" : "FAIL") + "\n" );
        count += ((last) ? 0: 1);

        last = testReadZeroSucceeds();
        SysLib.cout("Test 2: Read from Block 0 succeeds: " + ((last) ? "PASS" : "FAIL") + "\n" );
        count += ((last) ? 0: 1);

        last = testReadLastSucceeds();
        SysLib.cout("Test 2: Read from Block 0 succeeds: " + ((last) ? "PASS" : "FAIL") + "\n" );
        count += ((last) ? 0: 1);

        // Output totals
        SysLib.cout("TOTAL FAILURES: " + count + "\n");

        // Exit cleanly
        SysLib.exit();
    }

}
