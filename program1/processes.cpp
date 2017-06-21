// CSS 430 Program 1

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/wait.h>
#include <iostream>

using namespace std;

/*  Emulates `ps -A | grep argv[1] | wc - l` using fork(), execlp(), pipe(),
 *  etc.
 *
 */

int main(int argc, char *argv[]){

    // 1) Handle input

    cout << argv[1] << endl;

    // 2) Set up pipe for parent/child1

    // 3) Spawn first child

    // 3.A) if first child

    execlp("/bin/ps", "ps", "-A"); 
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
