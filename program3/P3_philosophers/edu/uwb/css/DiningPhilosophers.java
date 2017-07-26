package edu.uwb.css;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * Created by mike on 7/11/2016.
 */
public class DiningPhilosophers {
    DiningState[] state;
    private final Semaphore [] forks;
    //private final ReentrantLock mutex = new ReentrantLock();

    // This will let all the tests run (and fail)
    // You'll want to remove it once you actually create an array :)
    int nPhil;
    public DiningPhilosophers(int nPhilosophers) {
        nPhil =  nPhilosophers;

        // Initialize array of Philosophers
        state = new DiningState[nPhil];

        // Tautalogically, there are n eating implements for n Philosophers
        forks = new Semaphore[nPhil];

        for (int i = 0; i < nPhil; ++i){
            // Initialize state of each Philosopher
            state[i] = DiningState.THINKING;

            // Initialize forks
            forks[i] = new Semaphore(1); // A fork can only be held once
        }
    }

    /**
     * @brief   Attempts to acquire the forks to left and right of the philosopher indicated
     * @param[in] i      The Philosopher who is attempting to take forks to either side
     */
    public void takeForks(int i) {
        Main.TPrint( "TakeForks:   i=" + i);

    }

    public void returnForks(int i) {
        Main.TPrint( "returnForks:   i=" + i );
    }

    public int numPhilosophers() {
        return nPhil;
    }

    public DiningState getDiningState(int i) {
        return state[i];
    }
}
