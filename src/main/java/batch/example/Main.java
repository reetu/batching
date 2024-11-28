package batch.example;

import batch.*;
import batch.base.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
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

        /*
         * Here we process a single job and return the result. Addresses criteria:
         *
         * "the endpoint responds with data pertaining to each individual object"
         */

        System.out.println(Thread.currentThread() + "    Executing a job and returning a result");
        return new TelemetryJobResult<>("Successful result from single job");
    }
}


class TelemetryProcessor extends AbstractBatchProcessor<TelemetryJob> {

    public TelemetryProcessor(ProcessorConfig config) {
        super(config);
    }

    @Override
    public void dispatch(List<TelemetryJob> jobs) {
        System.out.println(Thread.currentThread() + "  Starting dispatch...");

        ConcurrentLinkedQueue<TelemetryJobResult<TelemetryJob>> jobResults = new ConcurrentLinkedQueue<>();

        // Get result of each Job (but do not notify of the result - this is now done after the whole batch is processed)
        List<CompletableFuture<Void>> futures = jobs.stream()
            .map(job -> CompletableFuture
                .supplyAsync(() -> job.process())
                .thenAccept(jobResult -> {
                    jobResults.add(jobResult);
                }).exceptionally(throwable -> {
                    job.getFailCallback().accept(throwable);
                    return null;
                }))
            .toList();


        /*
         * Afterward, we optionally perform further operations with all JobResults in the batch.
         * Addresses criteria: "combining batches of multiple objects into a single request".
         *
         * This approach gives the external user control over how they want Jobs to be processed.
         * i.e. individually, as a batch only, or individually + batched (as per this example)
         *
         * Note:  this was not included in the original submission as the assessment explicitly
         * stated "don't implement the scenario".
         */
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.thenRun(() -> {
            jobResults.forEach(job -> {
                // Now you can make an API call using all JobResults in the batch
            });
            String mockApiResponse = "Successfully processed all jobs";

            // Then notify each Job of the batch result
            jobs.forEach(job -> job.getSuccessCallback().accept(new TelemetryJobResult<>(mockApiResponse)));
        });

        System.out.println(Thread.currentThread() + "  Dispatch complete.\n" );
    }
}