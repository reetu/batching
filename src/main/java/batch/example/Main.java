package batch.example;

import batch.*;
import batch.base.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        ProcessorConfig config = new BatchProcessorConfig(2000, 3, 500);
        Processor<TelemetryJob> processor = new TelemetryProcessor(config);
        processor.start();

        Consumer<JobResult<TelemetryJob>> success = jobResult -> {
            System.out.println(Thread.currentThread() + "      Callback for " + jobResult);
        };
        Consumer<Throwable> failure = exception -> {
            System.out.println(Thread.currentThread() + "      Job failed with " + exception);
        };

        processor.submit(new TelemetryJob(success, failure));
        processor.submit(new TelemetryJob(success, failure));
        processor.submit(new TelemetryJob(success, failure));
        processor.submit(new TelemetryJob(success, failure));
        processor.submit(new TelemetryJob(success, failure));
        processor.submit(new TelemetryJob(success, failure));
        processor.submit(new TelemetryJob(success, failure));
        processor.submit(new TelemetryJob(success, failure));
    }
}



record TelemetryJobResult<J extends Job<J>> (String output) implements JobResult<J> {}

class TelemetryJob extends AbstractJob<TelemetryJob> {
    public TelemetryJob(Consumer<JobResult<TelemetryJob>> success, Consumer<Throwable> failure) {
        super(success, failure);
    }

    @Override
    public TelemetryJobResult<TelemetryJob> process() {
        // TODO: Job execution logic goes here
        System.out.println(Thread.currentThread() + "    Executing a job and returning a result");
        return new TelemetryJobResult<>("outputVal");
    }
}


class TelemetryProcessor extends AbstractBatchProcessor<TelemetryJob> {

    public TelemetryProcessor(ProcessorConfig config) {
        super(config);
    }

    @Override
    public void dispatch(List<TelemetryJob> jobs) {
        System.out.println(Thread.currentThread() + "  Starting dispatch...");
        jobs.forEach(job -> {
            CompletableFuture
                .supplyAsync(() -> job.process())
                .thenAccept(jobResult -> job.getSuccessCallback().accept(jobResult))
                .exceptionally(throwable -> {
                    job.getFailCallback().accept(throwable);
                    return null;
                });
        });
        System.out.println(Thread.currentThread() + "  Dispatch complete.\n" );
    }
}