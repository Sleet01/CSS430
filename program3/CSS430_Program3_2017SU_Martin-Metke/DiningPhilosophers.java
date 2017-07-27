package edu.uwb.css;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Monitor-like object implemented using a ReentrantLock instance and N "fork" conditions
 * (where N is the number of DiningPhilosophers) specifically for the DiningPhilosopher problem.
 *
 * @author      Martin L. Metke
 * @version     1.0
 * @date        2017/07/26
 */
public class DiningPhilosophers {
    // State of each Philosopher
    DiningState[] state;
    // Records if forks are available or being used
    private final boolean [] available;
    // Locks critical sections (like synchronized, but does not require a specific object or code block
    private final ReentrantLock lock = new ReentrantLock();
    // Conditions representing each fork, which allows Philosophers to wait until a fork's state changes
    private final Condition[] forks;
    // Waiting variables, for tracking how long neighbors have been waiting to take forks
    private final int [] waiting;

    private int nPhil;

    /**
     * @brief   Constructor; takes a number of Philosophers to seat.
     * @post    This DiningPhilosophers monitor will be set up to handle the number of Philosophers specified
     * @param nPhilosophers     The number of Philosophers (and by extension, forks) at the table
     */
    public DiningPhilosophers(int nPhilosophers) {
        nPhil =  nPhilosophers;

        // Initialize array of Philosophers
        state = new DiningState[nPhil];

        // Tautalogically, there are n eating implements for n Philosophers
        forks = new Condition [nPhil];

        // Allow threads to check current state of forks
        available = new boolean [nPhil];

        // Keep track of how long each Philosopher has been waiting and only allow the longer-waiting philosopher
        // to take a fork
        waiting = new int [nPhil];

        for (int i = 0; i < nPhil; ++i){
            // Initialize state of each Philosopher
            state[i] = DiningState.THINKING;

            // Initialize forks as Conditions on the lock object
            forks[i] = lock.newCondition(); // A fork can only be held once

            // Initialize availability of forks
            available[i] = true;

            // Set waiting to 0 for each seat
            waiting[i] = 0;
        }
    }

    /**
     * @brief   Attempts to acquire the forks to left and right of the philosopher indicated
     * @pre     This has been instantiated.
     * @post    The specified Philosopher will acquire the left and right fork, possibly after some waiting.
     * @param[in] i      The Philosopher who is attempting to take forks to either side
     *
     * Technically, it should be difficult for two Philosophers to truly be in contention for the same Fork
     * and have one Philosopher starved out, because only for every Fork there can only be one thread waiting for it;
     * all other threads are either not waiting for that Fork, or using that Fork, by definition.
     * However, because I chose not to instantiate the ReentrantLock as "fair" or to use a "fair" Semaphore system,
     * I wanted to explicitly guarantee fair waiting for the forks.  In this case, rather than having a wait value for
     * each fork-Philosopher pair, each Philosopher increments her waiting value as long as she is hungry but not
     * eating.  This should prevent one thread returning and then re-taking forks repeatedly, starving the other
     * Philosopher.
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

                // Increment this Philosopher's wait time
                ++waiting[i];

                if(!(available[left]) || waiting[left] > waiting[i]) {
                    try {
                        Main.TPrint("TakeForks - WAITING ON LEFT:   i=" + i);

                        forks[left].await();
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
                if(!(available[right]) || waiting[right] > waiting[i]){
                    try {
                        Main.TPrint("TakeForks - WAITING ON RIGHT:   i=" + i);

                        forks[right].await();
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }
            // Mark right and left forks as unavailable
            available[right] = available[left] = false;
            // No longer waiting to eat
            waiting[i] = 0;
            // state is now EATING
            state[i] = DiningState.EATING;
        }
        finally{
            lock.unlock();
        }
    }

    /**
     * @brief       Tell Philosopher "i" to return her forks, notifying any waiting Philosopher
     *               that they are now available.
     * @pre         The specified Philosopher currently holds the forks.
     * @param i     The Philosopher who is now done eating and wishes to return her forks.
     */
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

            // After returning the forks,
            state[i] = DiningState.THINKING;
        }
        finally{
            lock.unlock();
        }
    }

    /**
     * @brief       return the number of Philosophers that this intance tracks
     * @return      nPhil       Integer number of tracked Philosophers
     */
    public int numPhilosophers() {
        return nPhil;
    }

    /**
     * @brief       Return the state of any
     * @param i     The Philosopher of which to find the state
     * @return      state   DiningState enum value
     */
    public DiningState getDiningState(int i) {
        if (i >= 0 && i < this.nPhil) {
            return state[i];
        }
        else return null;
    }
}
