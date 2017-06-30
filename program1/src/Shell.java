/**
 * @author Martin L. Metke
 *
 *
 */

import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class Shell extends Thread {

    private int cmdNumber;
    private Pattern seqDelimiter;
    private Pattern consecDelimiter;

    public Shell()  {

        // Set up cmdNum starting point
        cmdNumber = 1;

        // set up regex patterns
        seqDelimiter = Pattern.compile(" +;+ +");
        consecDelimiter = Pattern.compile(" +&+ +");
    }

    public void run() {

        StringBuffer userCommand = new StringBuffer("");
        String [] sequentialChunks;
        String [] consecutiveChunks;

        // Enter a continuous loop of reading user input and executing commands
        while(true){
        
            // output prompt
            SysLib.cout("Shell[" + cmdNumber++ + "]% ");

            // take in user input (continuously re-check until length of cin > 0
            while ( SysLib.cin(userCommand) <= 0);

            // split input on ' ; ' (or something similar)
            // Splits the user input (cast into a string) into a string array
            sequentialChunks = seqDelimiter.split( userCommand.toString() );

            // For ;-delimited command line
            for( String chunk: sequentialChunks ) {

                consecutiveChunks = consecDelimiter.split(chunk);

                // for every &-delimited command
                for( String command: consecutiveChunks ) {

                    // if input == "exit": exit

                    // else record tid from exec(command)
                }

                // After last command, do "while( SysLib.join() != tid ){}"
            }
        }
    }
}
