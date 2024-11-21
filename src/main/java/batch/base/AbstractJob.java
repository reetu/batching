package batch.base;

import batch.Job;
import batch.JobResult;

import java.util.function.Consumer;

public abstract class AbstractJob<J extends Job<J>> implements Job<J> {

    private Consumer<JobResult<J>> successCallback;
    private Consumer<Throwable> failureCallback;

    // Private constructor ensures Jobs cannot be created without callbacks
    private AbstractJob() {}

    public AbstractJob(Consumer<JobResult<J>> successCallback, Consumer<Throwable> failureCallaback) {
        super();
        this.successCallback = successCallback;
        this.failureCallback = failureCallaback;
    }

    @Override
    public final Consumer<JobResult<J>> getSuccessCallback() {
        return this.successCallback;
    }

    @Override
    public final Consumer<Throwable> getFailCallback() {
        return this.failureCallback;
    }

    @Override
    public abstract JobResult<J> process();
}
