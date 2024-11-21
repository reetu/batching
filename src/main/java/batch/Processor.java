package batch;

import java.util.List;
import java.util.concurrent.BlockingQueue;

public interface Processor<J extends Job<J>> {

    /**
     * Starts the batch Processor.
     */
    void start();

    /**
     * Submits a Job to this batch Processor.
     *
     * @param job
     */
    void submit(J job);

    /**
     * Perform an action just before a Job is dispatched (i.e. processed by calling Job.execute())
     *
     * @param job The job to be dispatched
     * @return job The job to be dispatched
     */
    J beforeDispatch(J job);

    /**
     * The dispatching logic for a Jobs batch i.e. the action we will take to process a group of Jobs.
     *
     * @param jobs The Jobs to be processed.
     */
    void dispatch(List<J> jobs);

    /**
     * @return Tht config for this Processor.
     */
    ProcessorConfig getConfig();

    /**
     * @return The queue containing jobs that are waiting for dispatch.
     */
    BlockingQueue<J> getJobQueue();

}