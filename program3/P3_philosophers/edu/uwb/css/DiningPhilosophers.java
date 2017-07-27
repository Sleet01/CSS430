package edu.uwb.css;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * Created by mike on 7/11/2016.
 */
public class DiningPhilosophers {
    DiningState[] state;
    private final boolean [] available;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition[] forks;

    // This will let all the tests run (and fail)
    // You'll want to remove it once you actually create an array :)
    int nPhil;
    public DiningPhilosophers(int nPhilosophers) {
        nPhil =  nPhilosophers;

        // Initialize array of Philosophers
        state = new DiningState[nPhil];

        // Tautalogically, there are n eating implements for n Philosophers
        forks = new Condition [nPhil];

        // Allow threads to check current state of forks
        available = new boolean [nPhil];

        for (int i = 0; i < nPhil; ++i){
            // Initialize state of each Philosopher
            state[i] = DiningState.THINKING;

            // Initialize forks as Conditions on the lock object
            forks[i] = lock.newCondition(); // A fork can only be held once

            // Initialize availability of forks
            available[i] = true;
        }
    }

    /**
     * @brief   Attempts to acquire the forks to left and right of the philosopher indicated
     * @param[in] i      The Philosopher who is attempting to take forks to either side
     */
    public void takeForks(int i) {
        Main.TPrint( "TakeForks:   i=" + i);
        int left = i;
        int right = ((nPhil -1) + i) % nPhil;

        lock.lock();

        try {

            // Test loop to see if the forks are available; if not, wait on them.
            while (!(available[left] && available[right])){
                // Set current state of Philosopher i to "HUNGRY", indicating that we are waiting to eat.
                state[i] = DiningState.HUNGRY;
                if(!(available[left])) {
                    try {
                        Main.TPrint("takeForks - WAITING ON LEFT:   i=" + i);

                        forks[left].await();
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
                if(!(available[right])){
                    try {
                        Main.TPrint("takeForks - WAITING ON RIGHT:   i=" + i);

                        forks[right].await();
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }
            // Mark right and left forks as unavailable
            available[right] = available[left] = false;
            state[i] = DiningState.EATING;
        }
        finally{
            lock.unlock();
        }
    }

    public void returnForks(int i) {
        Main.TPrint( "returnForks:   i=" + i );

        int left = i;
        int right = ((nPhil -1) + i) % nPhil;

        lock.lock();

        try{
            if (!available[left]) {
                available[left] = true;
            }
            forks[left].signal();

            if (!available[right]) {
                available[right] = true;
            }
            forks[right].signal();
            state[i] = DiningState.THINKING;
        }
        finally{
            lock.unlock();
        }
    }

    public int numPhilosophers() {
        return nPhil;
    }

    public DiningState getDiningState(int i) {
        return state[i];
    }
}
