/**
 *  @author Martin L. Metke
 *  @file   Shell.java
 *
 *  The Shell class effects a command-line interface, although currently only with
 *  two extended capabilities over the basic ThreadOS launcher:
 *  1)  '&' delimiter:  causes command to execute "in the background" or concurrently, e.g.
 *                      '<command1> <arg1> <arg2> & <command2>' runs command 1 and command 2
 *                      simultaneously.
 *  2)  ';' delimiter:  causes command to execute sequentially, waiting for the command to complete.
 *                      '<command1> <arg1> <arg2> ; <command2>' runs command 1 first, then command 2
 *
 *  Edge case behavior:
 *      -   Multiple identical delimiters with no spaces between them are treated as just one delimiter
 *      -   "Blank" commands (only spaces, or null strings) will not be executed
 *      -   A command without a terminal delimiter is treated as delimited by a ';' character (sequential)
 *      -   Command invocation validation is left to the commands themselves; the Shell attempts to
 *          detect when a command cannot be executed but parameter validation is left to commands and
 *          ThreadOS handles missing commands.
 */

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 *  @brief  Shell for executing ThreadOS "commands" ( compiled .class files which extend Thread as well)
 */
public class Shell extends Thread {

    private int cmdNumber;
    private Pattern seqDelimiter;
    private Pattern concurDelimiter;

    /** @brief  Constructor for Shell class; sets some internal state information
     *  @pre    ThreadOS is running and loader (or another Shell instance) is running
     *  @post   Shell instance is created and its state info (delims, command number) set to initial values
     *  @return this
     *
     */
    public Shell()  {

        // Set up cmdNum starting point
        cmdNumber = 1;

        // set up regex patterns
        //seqDelimiter = Pattern.compile(" +;+ +");
        //concurDelimiter = Pattern.compile(" +&+ +");
        seqDelimiter = Pattern.compile(" *;+ *");
        concurDelimiter = Pattern.compile(" *&+ *");
    }

    /** @brief  'Run' function of the Shell class; required when extending Thread and used by ThreadOS
     *  @pre    ThreadOS is running and loader (or another Shell instance) is running
     *  @post   Shell instance will be running and accepting "commands", actually invocations of other .class
     *          -based commands
     *  @return void
     *
     *  @note   See http://docs.oracle.com/javase/1.5.0/docs/guide/misc/threadPrimitiveDeprecation.html
     *          for info about Thread.stop() being deprecated, possibly no longer supported in Java 8
     */
    public void run() {

        StringBuffer userCommand;
        String [] sequentialChunks;
        String [] concurrentChunks;
        int tid = -1;

        // Print welcome message
        SysLib.cout("\nNew Shell Created!  Type 'exit' to exit Shell");

        // Enter a continuous loop of reading user input and executing commands
        while(true){
        
            // output prompt
            SysLib.cout("\nShell[" + cmdNumber++ + "]% ");

            // Clear userCommand to alleviate repeated commands
            userCommand = new StringBuffer("");

            // take in user input (continuously re-check until length of cin > 0)
            while ( SysLib.cin(userCommand) < 0);

            // split input on ' ; ' (or something similar)
            // Splits the user input (cast into a string) into a string array
            sequentialChunks = seqDelimiter.split( userCommand.toString() );

            // For ;-delimited command line
            for( String chunk: sequentialChunks ) {

                // Split current line (ending in sequential command due to prior split on ";")
                concurrentChunks = concurDelimiter.split(chunk);

                // for every &-delimited command - consists of N concurrent commands and one sequential command
                for( String command: concurrentChunks ) {

                    // Take no action if command is empty or just whitespace
                    if ( !(command.equals("") || command.replaceAll("\\s+","").equals("") )) {

                        // if input == "exit": exit
                        if( command.equals("exit") ){
                            // *Attempt* to have SysLib clean up this' thread; may not be working in Java 8
                            SysLib.exit();
                            // Returning is the only possible way to ensure that execution of this thread
                            // ends, and control returns to the calling thread!
                            return;
                        }
                        else{
                            // else record tid from exec(command); only the last (which is sequential command)
                            // needs to be checked.
                            tid = SysLib.exec(SysLib.stringToArgs(command));
                            if (tid == -1){
                                SysLib.cout("\nINVALID COMMAND : '" + command + "\n");
                            }
                        }
                    }
                }

                // Only attempt to join() if the most recent sequential thread successfully executed
                if(tid != -1){
                    // After last command, do "while( SysLib.join() != tid ){}"
                    while( SysLib.join() != tid);
                }
            }
        }
    }
}
