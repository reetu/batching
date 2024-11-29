package batch.base;

import batch.Job;
import batch.JobResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.*;

public class AbstractBatchProcessorTest {

    // Minimal implementation of AbstractJob
    private class MyJob extends AbstractJob<MyJob> {
        public MyJob(Consumer<JobResult<MyJob>> success, Consumer<Throwable> failure) {
            super(success, failure);
        }

        @Override
        public JobResult<MyJob> process() {
            return null;
        }
    }

    private MyJob job;

    @BeforeEach
    void setUp() {
        job = mock(MyJob.class);
    }

    @Test
    void testGetConfig() {
        BatchProcessorConfig config = new BatchProcessorConfig(10, 2, 10);
        AbstractBatchProcessor<MyJob> processor = buildProcessor(config);

        assertEquals(config, processor.getConfig());
    }

    @Test
    void testGetJobQueue() {
        AbstractBatchProcessor<MyJob> processor = buildProcessor();

        assertNotNull(processor.getJobQueue());
    }

    @Test
    void testStart() {
        AbstractBatchProcessor<MyJob> processor = buildProcessor();
        assertFalse(processor.isStarted());

        processor.start();

        assertTrue(processor.isStarted());
    }

    @Test
    void testSubmit() throws InterruptedException {
        AbstractBatchProcessor<MyJob> processor = buildProcessor();
        processor.submit(job);

        assertEquals(processor.getJobQueue().size(), 1);
        assertEquals(processor.getJobQueue().take(), job);
    }

    @Test
    void testBeforeDispatch() {
        AbstractBatchProcessor<MyJob> processor = buildProcessor();

        MyJob result = processor.beforeDispatch(job);
        assertEquals(result, job);
    }

    @Test
    void testReadyToDispatch_notStarted() throws InterruptedException{
        AbstractBatchProcessor<MyJob> processor = buildProcessor();
        Thread.sleep(200);

        assertFalse(processor.readyToDispatch());
    }

    @Test
    void testReadyToDispatch_notStartedJobSubmitted() throws InterruptedException {
        AbstractBatchProcessor<MyJob> processor = buildProcessor();
        processor.submit(job);
        Thread.sleep(200);

        assertFalse(processor.readyToDispatch());
    }

    @Test
    void testReadyToDispatch_startedNoJobsSubmitted() throws InterruptedException {
        int interval = 10;
        AbstractBatchProcessor<MyJob> processor = buildProcessor(new BatchProcessorConfig(interval, 2, interval));
        processor.start();
        Thread.sleep(50);

        assertTrue(processor.readyToDispatch());
    }

    @Test
    void testReadyToDispatch_batchTooSmallBeforePollThreshold() throws InterruptedException {
        int interval = 5000;

        AbstractBatchProcessor<MyJob> processor = buildProcessor(new BatchProcessorConfig(interval, 2, interval));
        processor.submit(job);
        processor.start();

        assertFalse(processor.readyToDispatch());
    }

    @Test
    void testReadyToDispatch_batchTooSmallAfterPollThreshold() throws InterruptedException {
        int interval = 1;

        AbstractBatchProcessor<MyJob> processor = buildProcessor(new BatchProcessorConfig(interval, 2, interval));
        processor.submit(job);
        processor.start();
        Thread.sleep(10);

        assertTrue(processor.readyToDispatch());
    }

    @Test
    void testReadyToDispatch_batchThresholdReachedBeforePollThreshold() throws InterruptedException {
        int interval = 5000;

        AbstractBatchProcessor<MyJob> processor = buildProcessor(new BatchProcessorConfig(interval, 2, interval));
        processor.start();
        processor.submit(job);
        processor.submit(job);

        assertTrue(processor.readyToDispatch());
    }

    @Test
    void testDispatchSuccess() throws InterruptedException {
        record MyJobResult(String result) implements JobResult<MyJob>{}
        BatchProcessorConfig config = new BatchProcessorConfig(1, 1, 1);
        AbstractBatchProcessor<MyJob> processor = new AbstractBatchProcessor<MyJob>(config) {
            @Override
            public void dispatch(List<MyJob> jobs) {
                jobs.forEach(job -> job.getSuccessCallback().accept(new MyJobResult("Result ok")));
            }
        };
        processor.start();

        MyJob job1 = new MyJob(mock(Consumer.class), mock(Consumer.class));
        processor.submit(job1);

        // Wait for processor to batch
        Thread.sleep(100);

        verify(job1.getSuccessCallback()).accept(any(JobResult.class));
    }

    private AbstractBatchProcessor<MyJob> buildProcessor() {
        BatchProcessorConfig config = new BatchProcessorConfig(100, 2, 100);
        return mock(AbstractBatchProcessor.class, withSettings()
                .useConstructor(config)
                .defaultAnswer(CALLS_REAL_METHODS));
    }

    private AbstractBatchProcessor<MyJob> buildProcessor(BatchProcessorConfig config) {
         return mock(AbstractBatchProcessor.class, withSettings()
                .useConstructor(config)
                .defaultAnswer(CALLS_REAL_METHODS));
    }

}
