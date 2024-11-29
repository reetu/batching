package batch;

public interface ProcessorConfig {

    /**
     * @return Interval (in milliseconds) between Processor.dispatch() calls.
     */
    long batchInterval();

    /**
     * @return Maximum number of Jobs a Processor will dispatch in a single batch.
     */
    int batchSize();

    /**
     * Ideally set lower than batchInterval, so we can check if a batch is ready for early dispatch, i.e. earlier
     * than the standard batchInterval.
     *
     * @return Interval (in milliseconds) after which we check if a batch is ready to be dispatched.
     */
    long pollInterval();
}
