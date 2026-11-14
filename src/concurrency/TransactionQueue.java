package concurrency;

import java.util.LinkedList;

/**
 * Thread-safe bounded queue coordinated using intrinsic monitor locks,
 * synchronized methods, wait(), and notifyAll().
 *
 * Demonstrates:
 * - Milestone 4: Producer-Consumer pattern
 * - Condition-checking inside while loops to prevent spurious wakeups
 * - Monitor lock ownership
 */
public class TransactionQueue {
    private final LinkedList<TransactionTask> queue = new LinkedList<>();
    private final int capacity;
    private boolean stopped = false;

    public TransactionQueue(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void enqueue(TransactionTask task) throws InterruptedException {
        while (queue.size() >= capacity && !stopped) {
            System.out.printf("   [QUEUE FULL] Producer %s waiting for capacity...%n", Thread.currentThread().getName());
            wait();
        }
        if (stopped) {
            return;
        }
        queue.addLast(task);
        System.out.printf("   [ENQUEUED] %s added task %s (Queue size: %d/%d)%n",
                Thread.currentThread().getName(), task.getTaskId(), queue.size(), capacity);
        notifyAll();
    }

    public synchronized TransactionTask dequeue() throws InterruptedException {
        while (queue.isEmpty() && !stopped) {
            wait();
        }
        if (queue.isEmpty() && stopped) {
            return null;
        }
        TransactionTask task = queue.removeFirst();
        System.out.printf("   [DEQUEUED] %s took task %s (Remaining: %d)%n",
                Thread.currentThread().getName(), task.getTaskId(), queue.size());
        notifyAll();
        return task;
    }

    public synchronized void stop() {
        this.stopped = true;
        notifyAll();
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized boolean isStopped() {
        return stopped;
    }
}
