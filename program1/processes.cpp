// CSS 430 Program 1

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/wait.h>
#include <iostream>
#include <cstring>

using namespace std;

/*  Emulates `ps -A | grep argv[1] | wc -l` using fork(), execlp(), pipe(),
 *  etc.
 *
 */

int main(int argc, char *argv[]){

    // 1) Handle input
    cout << argv[1] << endl;

    if(argc != 2){
        cout << "Incorrect number of arguments; aborting!" << endl;
        return 1;
    }
    // strcmp returns 0 if strings match, which is boolean False
    else if (!strcmp(argv[1], "-h") || !strcmp(argv[1], "--help")){
        cout << "Usage: \'processes <search term> | -h/--help\'" << endl << endl;
        return 0;
    }

    string argument1(argv[1]);
    string fullCMD = "ps -A | grep " + argument1 + " | wc -l";

    cout << fullCMD << endl;

    // 2) Set up pipe for parent/child1

    // 3) Spawn first child

    // 3.A) if first child

    
    execlp("ps", "ps", "-A"); 
            // A.1) Set up pipe for child/grandchild

            // A.2) Spawn grandchild

            // A.3.a) if grandchild

                // a.1) set up pipe for grandchild/great-grandchild

                // a.2) spawn great-grandchild

                // a.2.i) if great-grandchild

                // a.2.ii) else (grandchild)

            // A.3.b) else (child)

    // 3.B) else (parent)
	
};
