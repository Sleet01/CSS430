import java.util.Date;



class Test4 extends Thread {

    private final static int OK = Kernel.OK;
    private final static int ERROR = Kernel.ERROR;
    private int suite;
    private boolean caching;
    private int instError = 0;

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

    public boolean testBadReadFails(){
        boolean result = false;
        if(caching) {
            result = (SysLib.cread(-1, new byte[512]) == ERROR);
            result = result & (SysLib.cread(1000, new byte[512]) == ERROR);
        } else {
            result = (SysLib.rawread(-1, new byte[512]) == ERROR);
            result = result & (SysLib.rawread(1000, new byte[512]) == ERROR);
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

    /**
     * @brief   Homework 4 test runner.  Ideally, I'd use a Command Pattern here, but I'm in a hurry.
     */
    public void run(){

        switch(this.instError) {
            case 0: break;
            case 1: {
                SysLib.cout("Incorrect Test4 invocation!  Use \"Test4 <enabled|disabled> <0-4>\"");
                SysLib.exit();
                break;
            }
            case 2: {
                SysLib.cout("Invalid caching selection!  Use \"enabled\" or \"disabled\"");
                SysLib.exit();
                break;
            }
            case 3: {
                SysLib.cout("Invalid suite selection!  Use \"0\" through \"4\"");
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
                SysLib.cout("Test 2: Read from Block 0 succeeds: " + ((last) ? "PASS" : "FAIL") + "\n");
                count += ((last) ? 0 : 1);

                // Output totals
                SysLib.cout("TOTAL FAILURES: " + count + "\n");

                break;

            }
            case 1: {
                SysLib.cout("This suite is not yet implemented!");
                break;
            }
            case 2: {
                SysLib.cout("This suite is not yet implemented!");
                break;
            }
            case 3: {
                SysLib.cout("This suite is not yet implemented!");
                break;
            }
            case 4: {
                SysLib.cout("This suite is not yet implemented!");
                break;
            }
        }

        // Exit cleanly
        SysLib.exit();
    }
}
