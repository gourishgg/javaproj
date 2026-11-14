package concurrency;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker thread that retrieves tasks from the queue and executes them.
 */
public class TransactionConsumer implements Runnable {
    private final String consumerName;
    private final TransactionQueue queue;
    private final AtomicInteger processedCount = new AtomicInteger(0);

    public TransactionConsumer(String consumerName, TransactionQueue queue) {
        this.consumerName = consumerName;
        this.queue = queue;
    }

    @Override
    public void run() {
        Thread.currentThread().setName(consumerName);
        System.out.printf(">> Consumer [%s] started and awaiting work.%n", consumerName);
        try {
            while (!Thread.currentThread().isInterrupted()) {
                TransactionTask task = queue.dequeue();
                if (task == null) {
                    // Queue stopped and drained
                    break;
                }
                task.run();
                processedCount.incrementAndGet();
            }
            System.out.printf(">> Consumer [%s] finished. Total processed: %d%n",
                    consumerName, processedCount.get());
        } catch (InterruptedException e) {
            System.out.printf(">> Consumer [%s] interrupted during dequeue.%n", consumerName);
            Thread.currentThread().interrupt();
        }
    }

    public int getProcessedCount() {
        return processedCount.get();
    }
}
