import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Scheduler extends Thread {
    private Vector queue;
    private ConcurrentLinkedQueue<TCB> queue0, queue1, queue2;
    private int timeSlice;
    private static final int DEFAULT_TIME_SLICE = 500;

    // New data added to p161 
    private int[] tids; // Indicate which ids have been used: -1 is unused; >= 0 is remaining quanta
    private static final int DEFAULT_MAX_THREADS = 10000;

    // A new feature added to p161 
    // Allocate an ID array, each element indicating if that id has been used
    private int nextId = 0;

    // Debugging output flag.  Set to "true" to enable CERR status output from the scheduler.
    // Clauses dependent on "static final" are removed at compilation if boolean is false.
    private static final boolean DEBUG_CERR = false;

    private void initTid(int maxThreads) {
        tids = new int[maxThreads];
        for (int i = 0; i < maxThreads; i++)
            tids[i] = -1;
    }

    // A new feature added to p161 
    // Search an available thread ID and provide a new thread with this ID
    synchronized private int getNewTid() {
        for (int i = 0; i < tids.length; i++) {
            int tentative = (nextId + i) % tids.length;
            // Modified from boolean in order to record remaining time quanta in the current queue
            // (for when q1+ threads are interrupted before reaching the end of their assigned time)
            if (tids[tentative] == -1) {
                tids[tentative] = 0;
                nextId = (tentative + 1) % tids.length;
                return tentative;
            }
        }
        return -1;
    }

    // A new feature added to p161 
    // Return the thread ID and set the corresponding tids element to be unused
    synchronized private boolean returnTid(int tid) {
        if (tid >= 0 && tid < tids.length && tids[tid] >= 0) {
            // Instead of "false", changes the tid entry to -1 (0+ show the remaining time quanta)
            tids[tid] = -1;

            // TO-DO: figure out how this causes a deadlock on QueueNode!
            // Reset "next TID" to the returned TID.  Hopefully this makes the scheduler and TestN threads
            // more responsive (see schedulerSleep() modifications)
            //nextId = tid;

            if(DEBUG_CERR) SysLib.cerr("CERR >> Returned TID; new nextId = " + tid + "\n");
            return true;
        }
        return false;
    }


    /**
     * @brief   New wrapper for returnTid that also handles queue removal
     * @param deadTCB       TCB that has been terminated and needs to be removed from queues
     * @param queueNum      The MLFB queue from which the TCB must be removed (known to the Scheduler)
     *
     * New private method to handle removing a terminated thread from both the general queue
     * and the correct priority queue
     */
    private void terminateThread(TCB deadTCB, int queueNum){

        // The deadTCB can only possibly be at the front of a queue, due to where terminateThread()
        // is called within the Scheduler's Run method.  Otherwise a more complex method for removing the
        // thread would be necessary
        try{
            switch(queueNum) {
                case 0: queue0.remove(deadTCB);
                        break;
                case 1: queue1.remove(deadTCB);
                        break;
                case 2: queue2.remove(deadTCB);
                        break;
            }
        } catch (NoSuchElementException e){
            SysLib.cerr(e.toString());
        }

        queue.remove(deadTCB);
        returnTid(deadTCB.getTid());
    }

    // A new feature added to p161 
    // Retrieve the current thread's TCB from the queue

    /**
     * @brief   getMyTcb: since I kept queue, this method needed no changes
     * @return  tcb     TCB of the current thread, or null
     */
    public TCB getMyTcb() {
        Thread myThread = Thread.currentThread(); // Get my thread object
        synchronized (queue) {
            for (int i = 0; i < queue.size(); i++) {
                TCB tcb = (TCB) queue.elementAt(i);
                Thread thread = tcb.getThread();
                if (thread == myThread) // if this is my TCB, return it
                    return tcb;
            }
        }
        return null;
    }

    // A new feature added to p161 
    // Return the maximal number of threads to be spawned in the system
    public int getMaxThreads() {
        return tids.length;
    }


    /**
     * @brief   Consolidated shared initialization code between the three Constructors
     * @param slice     int indicating the desired time slices for Q0 (Q1 = 2x, Q2 = 4x)
     * @param threads   int indicating the desired maximum number of threads to be scheduled
     */
    private void initScheduler(int slice, int threads){

        timeSlice = slice;

        queue = new Vector();
        queue0 = new ConcurrentLinkedQueue<TCB>();
        queue1 = new ConcurrentLinkedQueue<TCB>();
        queue2 = new ConcurrentLinkedQueue<TCB>();

        initTid(threads);

    }


    /**
     *  @brief   Default Constructor; uses DEFAULT time slice and max threads
     */
    public Scheduler() {

        this.initScheduler(DEFAULT_TIME_SLICE, DEFAULT_MAX_THREADS);

    }

    /**
     *  @brief   Optional Quantum-defining Constructor; uses specified timeslice and DEFAULT max threads
     */
    public Scheduler(int quantum) {

        this.initScheduler(quantum, DEFAULT_MAX_THREADS);

    }

    // A new feature added to p161 
    /**
     *  @brief   A constructor to receive the desired timeslice and max number of threads to be spawned
     */
    public Scheduler(int quantum, int maxThreads) {

        this.initScheduler(quantum, maxThreads);

    }

    private void schedulerSleep() {
        try {
            Thread.sleep(timeSlice);
        } catch (InterruptedException e) {
            SysLib.cerr(e.toString());
        }
    }

    /**
     * @brief   Overloaded schedulerSleep function that can take an arbitrary time slice
     * @param slice     int # of ms to sleep the current thread.
     */
    private void schedulerSleep(int slice) {
        try {
            Thread.sleep(slice);
        } catch (InterruptedException e) {
            SysLib.cerr(e.toString());
        }
    }

    // A modified addThread of p161 example
    public TCB addThread(Thread t) {
        // removed per instructions
        // setPriority(2)
        TCB parentTcb = getMyTcb(); // get my TCB and find my TID
        int pid = (parentTcb != null) ? parentTcb.getTid() : -1;
        int tid = getNewTid(); // get a new TID
        if (tid == -1)
            return null;
        TCB tcb = new TCB(t, tid, pid); // create a new TCB

        // Add tcb to the overall queue of all threads; it will remain here as long as it exists
        queue.add(tcb);

        // New for Program2:
        // All new threads are enqueued in Queue 0 first
        queue0.add(tcb);
        return tcb;
    }

    // A new feature added to p161
    // Removing the TCB of a terminating thread
    public boolean deleteThread() {
        TCB tcb = getMyTcb();
        if (tcb != null)
            return tcb.setTerminated();
        else
            return false;
    }

    public void sleepThread(int milliseconds) {
        try {
            sleep(milliseconds);
        } catch (InterruptedException e) {
            SysLib.cerr(e.toString());
        }
    }

    // A modified run of p161
    public void run() {

        // Added for Program2 assignment
        int quantaToRun;    // Stores the number of time quanta (time slices) to allocate to the next thread
        TCB nextThread;     // Stores the next thread to be run
        int currentQueue;   // Stores the current queue location of the next thread, for bookkeeping

        Thread current;

        // removed per instructions
        // setPriority(6)
        outer:
        while (true) {
            try {

                // Assign front of each queue to nextThread, which will:
                // A) ensure synchronized operations (peek() is synchronized; isEmpty() is not)
                // B) record if all queues are empty at run time
                if((nextThread = queue0.peek()) != null){
                    // Record the current running queue for later bookkeeping
                    currentQueue = 0;

                    if(DEBUG_CERR) SysLib.cerr("CERR >> Starting tid from Queue0: " + nextThread.getTid() + "\n");

                    // a thread in this queue gets 1 timeslice before being moved to the next queue
                    quantaToRun = 1;
                }
                else if ((nextThread = queue1.peek()) != null){
                    // Record the current running queue for later bookkeeping
                    currentQueue = 1;

                    if(DEBUG_CERR) SysLib.cerr("CERR >> Starting tid from Queue1: " + nextThread.getTid() + "\n");

                    // a thread in this queue gets 2 timeslices before being moved to the next queue
                    quantaToRun = 2;
                }
                else if ((nextThread = queue2.peek()) != null){
                    // Record the current running queue for later bookkeeping
                    currentQueue = 2;

                    if(DEBUG_CERR) SysLib.cerr("CERR >> Starting tid from Queue2: " + nextThread.getTid() + "\n");

                    // a thread in this queue gets 4 timeslices before being moved to the back of Queue2 again
                    quantaToRun = 4;
                }
                else{
                    continue;
                }

                TCB currentTCB = nextThread;

                // Update the number of quanta that the current thread will be allowed to run
                // (handles a thread getting interrupted partway through its quanta)
                synchronized (tids) {
                    if(tids[nextThread.getTid()] != 0) {
                         quantaToRun = tids[nextThread.getTid()];
                    }
                    else{
                        tids[nextThread.getTid()] = quantaToRun;
                    }
                }

                // Depending on the current queue level, run for a certain number of quanta.
                // This ensures Round-Robin execution for Queue2 threads with preemption by new
                // Queue0 and Queue1 threads at every time quantum
                for(int i=0; i < quantaToRun; ++i){
                    if(DEBUG_CERR) SysLib.cerr("CERR >> Starting TID " + nextThread.getTid() + " quanta " + (i+1) + " of " + quantaToRun + "\n");

                    // Check if thread has terminated between last quanta and now
                    if (currentTCB.getTerminated()) {
                        this.terminateThread(currentTCB, currentQueue);
                        if(DEBUG_CERR) SysLib.cerr("CERR >> Thread terminated; exiting for loop early\n");

                        break;  // break to outer "while" loop
                    }
                    else{ // The meat of Scheduler operations: start the next valid thread and sleep the Scheduler

                        current = currentTCB.getThread();

                        //
                        if (current != null) {
                            if (current.isAlive()) {
                                if(DEBUG_CERR) SysLib.cerr("CERR >> Alive thread resuming\n");
                                // changed to current.resume() per instructions
                                // current.setPriority(4)
                                current.resume();
                            }
                            else {
                                if(DEBUG_CERR) SysLib.cerr("CERR >> non-Alive thread starting\n");
                                // Spawn must be controlled by Scheduler
                                // Scheduler must start a new thread

                                // changed to current.resume() per instructions
                                // current.setPriority(4)
                                current.start();
                            }
                        }

                        if(DEBUG_CERR) SysLib.cerr("CERR >> Scheduler sleeping @ " + new Date().getTime() + "\n");

                        // Attempt to massage Thread speeds.  TID 0 is the Scheduler; TID 1 is *usually* the loaded
                        // class.  With the changes to make "nextId" drop back to 1 after the last loaded class exits,
                        // this should keep the sleep time for the Scheduler and .join() threads much lower.
                        if (nextThread.getTid() >= 2){
                            schedulerSleep();
                        }
                        else{
                            schedulerSleep(10);
                        }

                        if(DEBUG_CERR) SysLib.cerr("CERR >> Scheduler waking @ " + new Date().getTime() + "\n");

                        if (current != null && current.isAlive())
                            // changed to current.suspend() per instructions
                            // current.setPriority(2)
                            current.suspend();

                        // Decrement remaining quanta for the current thread and check value.
                        // If the thread is out of time quanta (e.g., it had 3 left after being preempted)
                        // then end the for loop and do post-execution cleanup before hitting the next "while"
                        // iteration.
                        synchronized(tids) {
                            if((--tids[currentTCB.getTid()]) == 0) {
                                if(DEBUG_CERR) SysLib.cerr("CERR >> Decrementing quanta for TID " + currentTCB.getTid() + " to 0\n");
                                break;
                            }
                            else {
                                if(DEBUG_CERR) SysLib.cerr("CERR >> Decrementing quanta for TID " + currentTCB.getTid() + " to " + tids[currentTCB.getTid()] + "\n");
                            }
                        }

                        // Finally, check if new threads have been enqueued during the current time quantum;
                        // if so, interrupt current thread and re-start scheduling from higher-level queue's thread
                        // This will leave the current thread at the front of its queue until the next time that queue
                        // is selected.
                        switch (currentQueue){
                            case 1: if(!(queue0.isEmpty())) {
                                        if(DEBUG_CERR) SysLib.cerr("CERR >> Queue1 thread preempted by Queue0 thread!\n");
                                        continue outer;
                                    }
                                    break;
                            case 2: if(!(queue0.isEmpty() && queue1.isEmpty())) {
                                        if(DEBUG_CERR) SysLib.cerr("CERR >> Queue2 thread preempted by Queue0/Queue1 thread!\n");
                                        continue outer;
                                    }
                                    break;
                            default: break;
                        }
                    }
                }

                // If we arrive here, the current thread has completed using its assigned time quanta.
                // Move the current thread to the next appropriate queue.
                switch (currentQueue) {
                    case 0: queue0.remove(currentTCB);
                            queue1.add(currentTCB);
                            break;
                    case 1: queue1.remove(currentTCB);
                            queue2.add(currentTCB);
                            break;
                    case 2: queue2.remove(currentTCB);
                            queue2.add(currentTCB);
                            break;
                }

            } catch (NullPointerException e3) {
                SysLib.cerr(e3.toString());
            }
            ;
            if(DEBUG_CERR) SysLib.cerr("CERR >> ======== Finished for loop ======== \n");
        }
    }
}
