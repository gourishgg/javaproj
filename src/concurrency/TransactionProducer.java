package concurrency;

import java.util.List;

/**
 * Worker thread that submits transactions into the shared bounded queue.
 */
public class TransactionProducer implements Runnable {
    private final String producerName;
    private final TransactionQueue queue;
    private final List<TransactionTask> tasksToProduce;

    public TransactionProducer(String producerName, TransactionQueue queue, List<TransactionTask> tasksToProduce) {
        this.producerName = producerName;
        this.queue = queue;
        this.tasksToProduce = tasksToProduce;
    }

    @Override
    public void run() {
        Thread.currentThread().setName(producerName);
        System.out.printf(">> Producer [%s] started generating %d transaction tasks.%n",
                producerName, tasksToProduce.size());
        try {
            for (TransactionTask task : tasksToProduce) {
                queue.enqueue(task);
                Thread.sleep(50); // Simulate ingestion gap
            }
            System.out.printf(">> Producer [%s] finished adding all tasks.%n", producerName);
        } catch (InterruptedException e) {
            System.out.printf(">> Producer [%s] was interrupted.%n", producerName);
            Thread.currentThread().interrupt();
        }
    }
}
