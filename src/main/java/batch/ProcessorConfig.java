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
     * @return Interval (in milliseconds) after which Processor.readyToDispatch() is called.
     */
    long pollInterval();
}
