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

    private void initTid(int maxThreads) {
        tids = new int[maxThreads];
        for (int i = 0; i < maxThreads; i++)
            tids[i] = -1;
    }

    // A new feature added to p161 
    // Search an available thread ID and provide a new thread with this ID
    private int getNewTid() {
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
    private boolean returnTid(int tid) {
        if (tid >= 0 && tid < tids.length && tids[tid] >= 0) {
            tids[tid] = -1;
            return true;
        }
        return false;
    }

    // New private method to handle removing a terminated thread from both the general queue
    // and the correct priority queue
    private void terminateThread(TCB deadTCB, int queueNum){

        // The deadTCB can only possibly be at the front of a queue, due to where terminateThread()
        // is called within the Scheduler's Run method.  Otherwise a more complex method for removing the
        // thread would be necessary
        try{
            switch(queueNum) {
                case 0: queue0.remove(deadTCB);
                case 1: queue1.remove(deadTCB);
                case 2: queue2.remove(deadTCB);
            }
        } catch (NoSuchElementException e){
            SysLib.cerr(e.toString());
        }

        queue.remove(deadTCB);
        returnTid(deadTCB.getTid());
    }

    // A new feature added to p161 
    // Retrieve the current thread's TCB from the queue
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

    // Consolidated shared initialization code between the three Constructors
    private void initScheduler(int slice, int threads){

        timeSlice = slice;

        queue = new Vector();
        queue0 = new ConcurrentLinkedQueue<TCB>();
        queue1 = new ConcurrentLinkedQueue<TCB>();
        queue2 = new ConcurrentLinkedQueue<TCB>();

        initTid(threads);

    }

    // Default Constructor; uses DEFAULT time slice and max threads
    public Scheduler() {

        this.initScheduler(DEFAULT_TIME_SLICE, DEFAULT_MAX_THREADS);

    }

    // Optional Quantum-defining Constructor; uses specified timeslice and DEFAULT max threads
    public Scheduler(int quantum) {

        this.initScheduler(quantum, DEFAULT_MAX_THREADS);

    }

    // A new feature added to p161 
    // A constructor to receive the max number of threads to be spawned
    public Scheduler(int quantum, int maxThreads) {

        this.initScheduler(quantum, maxThreads);

    }

    private void schedulerSleep() {
        try {
            Thread.sleep(timeSlice);
        } catch (InterruptedException e) {
        }
    }

    // A modified addThread of p161 example
    public TCB addThread(Thread t) {
        // removed per instructions
        TCB parentTcb = getMyTcb(); // get my TCB and find my TID
        int pid = (parentTcb != null) ? parentTcb.getTid() : -1;
        int tid = getNewTid(); // get a new TID
        if (tid == -1)
            return null;
        TCB tcb = new TCB(t, tid, pid); // create a new TCB
        // Add tcb to the overall queue of all threads; it will remain here as long as it exists
        queue.add(tcb);
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
        }
    }

    // A modified run of p161
    public void run() {

        // Added for Program2 assignment
        int quantaToRun;
        TCB nextThread;
        int currentQueue;

        Thread current = null;

        // removed per instructions
        while (true) {
            try {

                // Assign front of each queue to nextThread, which will:
                // A) ensure synchronized operations (peek() is synchronized; isEmpty() is not)
                // B) record if all queues are empty at run time
                if((nextThread = queue0.peek()) != null){
                    // Record the current running queue for later bookkeeping
                    currentQueue = 0;

                    // a thread in this queue gets 1 timeslice before being moved to the next queue
                    quantaToRun = 1;
                }
                else if ((nextThread = queue1.peek()) != null){
                    // Record the current running queue for later bookkeeping
                    currentQueue = 1;

                    // a thread in this queue gets 2 timeslices before being moved to the next queue
                    quantaToRun = 2;
                }
                else if ((nextThread = queue2.peek()) != null){
                    // Record the current running queue for later bookkeeping
                    currentQueue = 2;

                    // a thread in this queue gets 4 timeslices before being moved to the back of Queue2 again
                    quantaToRun = 4;
                }
                else {
                    continue; // should only occur when all queues are empty
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

                    // Check if thread has terminated between last quanta and now
                    if (currentTCB.getTerminated() == true) {
                        this.terminateThread(currentTCB, currentQueue);
                        break;  // break to outer "while" loop
                    }
                    else{ // The meat of Scheduler operations: start the next valid thread and sleep the Scheduler

                        current = currentTCB.getThread();

                        //
                        if (current != null) {
                            if (current.isAlive()) {
                                // changed to current.resume() per instructions
                                current.resume();
                            }
                            else {
                                // Spawn must be controlled by Scheduler
                                // Scheduler must start a new thread
                                current.start();
                            }
                        }

                        schedulerSleep();
                        // System.out.println("* * * Context Switch * * * ");

                        if (current != null && current.isAlive())
                            // changed to current.suspend() per instructions
                            current.suspend();

                        // Decrement remaining quanta for the current thread and check value.
                        // If the thread is out of time quanta (e.g., it had 3 left after being preempted)
                        // then end the for loop and do post-execution cleanup before hitting the next "while"
                        // iteration.
                        synchronized(tids) {
                            if((--tids[currentTCB.getTid()]) == 0)
                                break;
                        }

                        // Finally, check if new threads have been enqueued during the current time quantum;
                        // if so, interrupt current thread and re-start scheduling from higher-level queue's thread
                        // This will leave the current thread at the front of its queue until the next time that queue
                        // is selected.
                        switch (currentQueue){
                            case 1: if(!(queue0.isEmpty())) continue;
                            case 2: if(!(queue0.isEmpty() && queue1.isEmpty())) continue;
                        }
                    }
                }

                // If we arrive here, the current thread has completed using its assigned time quanta.
                // Move the current thread to the next appropriate queue.
                switch (currentQueue) {
                    case 0: queue0.remove(currentTCB);
                            queue1.add(currentTCB);
                    case 1: queue1.remove(currentTCB);
                            queue2.add(currentTCB);
                    case 2: queue2.remove(currentTCB);
                            queue2.add(currentTCB);
                }

            } catch (NullPointerException e3) {
            }
            ;
        }
    }
}
