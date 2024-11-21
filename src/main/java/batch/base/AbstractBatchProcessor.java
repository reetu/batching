package batch.base;

import batch.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public abstract class AbstractBatchProcessor<J extends Job<J>> implements Processor<J> {

    /**
     * Apply this multiplier to determine the max capacity of the jobQueue.
     */
    private static final int QUEUE_BUFFER_MULTIPLIER = 3;

    /**
     * Reference to the background long-running batch dispatcher thread
     */
    private final Thread dispatcherThread;

    /**
     * Millisecond timestamp of when last batch of events was dispatched.
     * Defaults to Long.MIN_VALUE if the Processor has not started.
     */
    private long lastDispatchTime = Long.MIN_VALUE;

    private final ProcessorConfig config;
    private final BlockingQueue<J> jobQueue;

    public AbstractBatchProcessor(ProcessorConfig config) {
        this.config = config;

        // Set an upper limit on the jobQueue. If the job queue becomes full, the queue will block.
        int maxQueueSize = this.config.batchSize() * QUEUE_BUFFER_MULTIPLIER;
        this.jobQueue = new LinkedBlockingQueue<>(maxQueueSize);


        // Create the dispatch thread but don't start it, as the current object has not finished instantiating.
        this.dispatcherThread = new Thread(createDispatcher());
    }

    private synchronized long getLastDispatchTime() {
        return this.lastDispatchTime;
    }

    private synchronized void setLastDispatchTime(long millis) {
        this.lastDispatchTime = millis;
    }

    private Runnable createDispatcher() {
        return () -> {
            while (true) {
                if (readyToDispatch()) {
                    List<J> batch = getBatchJobs().stream()
                            .map(job -> beforeDispatch(job))
                            .collect(Collectors.toList());

                    this.setLastDispatchTime(System.currentTimeMillis());
                    dispatch(batch);
                }

                try {
                    Thread.sleep(config.pollInterval());
                } catch (InterruptedException e) {
                    System.out.println("Batch dispatcher thread interrupted, cleaning up...");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        };
    }

    private List<J> getBatchJobs() {
        List<J> jobBatch = new ArrayList<>();
        this.jobQueue.drainTo(jobBatch, config.batchSize());
        return jobBatch;
    }

    /**
     * Checks if the Processor is dispatching Jobs.
     *
     * @return True if start() has been called, otherwise False.
     */
    protected boolean isStarted() {
        return this.dispatcherThread.isAlive();
    }

    /**
     * Determine if a batch should be dispatched. Batch may contain zero or more Jobs.
     *
     * @returns True if either the maximum batch size is reached (as defined by config.getBatchSize()) or the
     * maximum interval between dispatch events has elapsed (as defined by config.getBatchInterval()), otherwise False.
     */
    protected boolean readyToDispatch() {
        if (!isStarted()) {
            return false;
        }

        long timeSinceLastDispatch = System.currentTimeMillis() - this.getLastDispatchTime();
        boolean timeIntervalTrigger = timeSinceLastDispatch >= this.config.batchInterval();
        boolean queueSizeTrigger = this.jobQueue.size() >= this.config.batchSize();

        return timeIntervalTrigger || queueSizeTrigger;
    }

    @Override
    public ProcessorConfig getConfig() {
        return this.config;
    }

    @Override
    public BlockingQueue<J> getJobQueue() {
        return this.jobQueue;
    }

    @Override
    public final void start() {
        this.dispatcherThread.start();
        this.setLastDispatchTime(System.currentTimeMillis());
        System.out.println(Thread.currentThread()+ " Started batch dispatcher thread...");
    }

    @Override
    public final void submit(J job) {
        try {
            this.jobQueue.put(job);
        } catch (InterruptedException e) {
            System.out.println("Interrupted while adding job to batch queue.");
            Thread.currentThread().interrupt();
        }
    }

    @Override
    /**
     * Default implementation: no pre-processing actions before dispatching a Job.
     */
    public J beforeDispatch(J job) {
        return job;
    }

    @Override
    public abstract void dispatch(List<J> jobs);
}
