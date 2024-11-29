package batch;

import java.util.function.Consumer;

public interface Job<J extends Job<J>> {

    /**
     * The processing logic this job will perform.
     *
     * @return The JobResult of processing this Job
     */
    JobResult<J> process();

    /**
     * Optional success callback for use by asynchronous Processor implementations.
     *
     * @return On successful processing of the Job, the callback for the JobResult
     */
    Consumer<JobResult<J>> getSuccessCallback();

    /**
     * Optional failure callback for use by asynchronous Processor implementations.
     *
     * @return On Job failure, the callback for the Throwable Error or Exception
     */
    Consumer<Throwable> getFailCallback();
}
