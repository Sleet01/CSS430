// CSS 430 Program 1

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/wait.h>
#include <iostream>
#include <cstring>

using namespace std;

/*  @brief  Helper that wraps pipe-getting logic and reports errors
 *  @pre    fd is declared in the calling method
 *  @post   fd 
 */
bool getPipe(int fd[2]){
    
    bool got = false;
    
    if (pipe(fd) < 0){
        perror("Pipe error:");
    }
    else{
        got = true;
    }

    return got;
}

/*  Emulates `ps -A | grep argv[1] | wc -l` using fork(), execlp(), pipe(),
 *  etc.
 *
 */
int main(int argc, char *argv[]){

    // 1) Handle input

    if(argc != 2){
        cout << "Incorrect number of arguments; aborting!" << endl;
        return 1;
    }
    // strcmp returns 0 if strings match, which is boolean False
    else if (!strcmp(argv[1], "-h") || !strcmp(argv[1], "--help")){
        cout << argv[1] << endl;
        cout << "Usage: \'processes <search term> | -h/--help\'" << endl << endl;
        return 0;
    }

    // Create string version of argv[1] for debugging and output
    string argument1(argv[1]);
    string fullCMD = "ps -A | grep " + argument1 + " | wc -l";

    cout << "Running full command: " << fullCMD << endl;

    // 2) Prep for and Spawn first child
    int fd[2];
    pid_t pid;

    // Basic fork() error checking
    if((pid = fork()) < 0){
        perror("Fork error:");
        exit(2);
    }
    // 2.A) if first child
    else if (pid == 0){
        enum {RD, WR};

        // A.1) Set up pipe for child/grandchild
        // If we don't get a pipe, exit!
        if (!getPipe(fd)){
            exit(1);
        }

        // A.2) Spawn grandchild
        if((pid = fork()) < 0){
            perror("Fork error:");
            exit(2);
        }
        // A.3.a) if grandchild
        else if (pid == 0){

            // a.1) set up pipe for grandchild/great-grandchild
            int fdgc[2];
            // If we don't get a pipe, exit!
            if (!getPipe(fdgc)){
                exit(1);
            }

            // a.2) spawn great-grandchild
            if((pid = fork()) < 0){
                perror("Fork error (great-grandchild):");
                exit(2);
            }
            // a.2.i) if great-grandchild
            else if (pid == 0){
                cout << "Great-grandchild: executing partial command: ps -A" << endl;
                // Redirect pipe in accordance with Program1 Part1 specs
                // Close Great-GrandChild's RD end of Great-Grandchild/GrandChild pipe
                close(fdgc[RD]);
                // Close Great-GrandChild's connection to stdout
                close(WR);
                // Reassign the pipe's WR end to stdout fd
                dup2(fdgc[WR], WR);
                // Execute 'ps -a'
                execlp("ps", "ps", "-A", (char*)NULL);

            // a.2.ii) else (grandchild)
            }
            else{
                // Logging
                cout << "Grandchild: executing partial command: grep " << argument1 << endl;
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

                // cerr << "DEBUG: grandchild's copy of ARGV[1]: " << argument1 << endl;
                // cerr << "DEBUG: copy of ARGV[1] == 'kwork': " << ((!strcmp(argv[1], argument1.c_str())) ? "True" : "False" ) <<
                //    endl;

                execlp("grep", "grep", argument1.c_str(),(char*)NULL); 
            }

        } 
        // A.3.b) else (child)
        else {
            // Logging
            cout << "Child: Executing partial command: wc -l" << endl;
            // Redirect pipe in accordance with Program1 Part1 specs
            // Close Child1's Write end of the pipe.
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
        wait(NULL);
        cout << "Parent completed!" << endl;
	}
};
