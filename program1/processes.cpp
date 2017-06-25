/** 
 *  @file    processes.cpp
 *  @author  Martin Metke (sleet01@uw.edu)
 *  @date    2017/06/25  
 *  @version 1.0 
 *  
 *  @brief CSS430 Program 1 Part 1 - replicate "ps -A | grep <argv[1]> | wc -l"
 *
 *  @section DESCRIPTION
 *  
 *  This is a small program that replicates the process of spawning and piping
 *  child processes in a Linux shell.  In this case, the program first spawns
 *  a child to run "wc -l"; that process spawns a child which greps incoming
 *  text for lines matching the CLI argument passed to this file; *that* child
 *  also spawns a child which runs "ps -A" to output all running processes.
 *  Alternatively:
 *
 *  Great-grandchild "ps -A" outputs to:
 *      Grandchild "grep <argv[1]>" which outputs to:
 *          Child "wc -l" which outputs to stdout, which:
 *              Parent waits for (all children must complete)
 *  
 *  This file (once compiled) accepts one command-line argument which can be:
 *  1) -h / --help          Outputs usage options to the CLI, returns 0
 *  2) <string>             Any text without spaces, which this file will
 *                          search the output of "ps -A" for, and then pass
 *                          through "wc -l" to count the total instances of
 *                          processes matching that string.
 *
 */

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/wait.h>
#include <iostream>
#include <cstring>

using namespace std;

/*  @brief  Helper that wraps pipe-getting logic and reports errors
 *  @pre    fd is declared in the calling method
 *  @post   fd is modified with new file descriptors, or unmodified 
 *          (on failure).
 *  @param[out]     fd      int [] which will hold pipe FD numbers
 *  @return         got     true if pipe assigned; false if not
 */
bool getPipe(int fd[2]){
    
    bool got = false;
    
    // pipe() syscall returns -1 if failed, 0 otherwise
    if (pipe(fd) < 0){
        
        // print relevant error information
        perror("Pipe error:");
    }
    else{
        got = true;
    }

    return got;
}

/*  @brief  Emulates `ps -A | grep argv[1] | wc -l` using fork(), execlp(), pipe(),
 *          etc.
 *  @pre    this file is compiled and instantiated from a POSIX-compatible
 *          shell (bash, a Cygwin terminal, etc.)
 *  @post   N/A
 *  @param[in]  argc        count of command-line args.  This file's name is #0
 *  @param[in]  argv        char * array of CLI args.  This file's name is 
 *                          argv[0]
 *  @return     
 */
int main(int argc, char *argv[]){

    // 1) Handle input
    if(argc != 2){
        cerr << "Incorrect number of arguments; aborting!" << endl;
        return EXIT_FAILURE;
    }

    // strcmp returns 0 if strings match, which is boolean False
    else if (!strcmp(argv[1], "-h") || !strcmp(argv[1], "--help")){
        //Logging
        //cout << argv[1] << endl;
        cout << "Usage: \'processes <search term> | -h/--help\'" << endl << endl;
        return 0;
    }

    // Create string version of argv[1] for debugging and output
    string argument1(argv[1]);
    string fullCMD = "ps -A | grep " + argument1 + " | wc -l";

    // Logging
    //cout << "Running full command: " << fullCMD << endl;

    // 2) Prep for and Spawn first child
    int fd[2];
    pid_t pid;

    // Basic fork() error checking
    if((pid = fork()) < 0){
        perror("Fork error:");
        return EXIT_FAILURE;
    } // 2.A) if first child
    else if (pid == 0){
        
        // This makes pipe and FD addressing cleaner
        enum {RD, WR};

        // A.1) Set up pipe for child/grandchild
        // If we don't get a pipe, exit!
        if (!getPipe(fd)){
            return EXIT_FAILURE;
        }

        // A.2) Spawn grandchild
        // If fork fails, exit!
        if((pid = fork()) < 0){
            perror("Fork error (grandchild):");
            return EXIT_FAILURE;
        } // A.3.a) if grandchild
        else if (pid == 0){

            // a.1) set up pipe for grandchild/great-grandchild
            // We can re-use pid, but the two pipes must exist simultaneously
            // so a second pipe must be defined.
            int fdgc[2];

            // If we don't get a pipe, exit!
            if (!getPipe(fdgc)){
                return EXIT_FAILURE;
            }

            // a.2) spawn great-grandchild
            // Again, if forking fails, exit!
            if((pid = fork()) < 0){
                perror("Fork error (great-grandchild):");
                return EXIT_FAILURE;
            }// a.2.i) if great-grandchild
            else if (pid == 0){

                // Logging
                //cout << "Great-grandchild: executing partial command: ps -A" << endl;

                // Redirect pipe in accordance with Program1 Part1 specs
                // Close Great-GrandChild's RD end of Great-Grandchild/GrandChild pipe
                close(fdgc[RD]);

                // Close Great-GrandChild's connection to stdout
                close(WR);
                
                // Reassign the pipe's WR end to stdout fd
                dup2(fdgc[WR], WR);

                // Execute 'ps -a'
                execlp("ps", "ps", "-A", (char*)NULL);

            } // a.2.ii) else (grandchild)
            else{

                // Logging
                //cout << "Grandchild: executing partial command: grep " << argument1 << endl;

                // Redirect pipe in accordance with Program1 Part1 specs
                // Close Grandchild's RD end of the Grandchild/Child pipe.
                close(fd[RD]);

                // Close Grandchild's WR end of the Great-Grandchild/Grandchild pipe.
                close(fdgc[WR]);


                // Close GrandChild's connection to stdin.
                close(RD);

                // Close GrandChild's connection to stdout.
                close(WR);

                
                // Reassign the child/grandchild pipe's WR end to stdout fd
                dup2(fd[WR], WR);

                // Reassign the grandchild/great-grandchild pipe's RD end to stdin fd
                dup2(fdgc[RD], RD);

                // execlp only passes char * [] arguments, but argument1 is now
                // a c++ string, so we grab the underlying cstring to pass to execlp
                execlp("grep", "grep", argument1.c_str(),(char*)NULL); 
            }

        } 
        // A.3.b) else (child)
        else {

            // Logging
            //cout << "Child: Executing partial command: wc -l" << endl;

            // Redirect pipe in accordance with Program1 Part1 specs
            // Close Child1's WR end of the pipe.
            close(fd[WR]);

            // Close Child1's connection to stdin
            close(RD);

            // Reassign the pipe's RD end to stdin fd
            dup2(fd[RD], RD);
                    
            execlp("wc", "wc", "-l", (char*)NULL); 
        }

    } 
    // 2.B) else (parent)
    else {

        // Per Program 1 Part 1 spec, the parent process must wait for all children to complete. 
        wait(NULL);

        // Logging
        //cout << "Parent completed!" << endl;

        return EXIT_SUCCESS;
	}
};
