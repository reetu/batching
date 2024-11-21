package batch.base;

import batch.Job;
import batch.JobResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbstractJobTest {

    private AbstractJob<?> job;
    private Consumer<JobResult<?>> success;
    private Consumer<Throwable> failure;

    @BeforeEach
    void setUp() {
        success = mock(Consumer.class);
        failure = mock(Consumer.class);

        job = mock(AbstractJob.class, withSettings()
                .useConstructor(success, failure)
                .defaultAnswer(CALLS_REAL_METHODS));
    }

    @Test
    void testGetSuccessCallback() {
        assertEquals(success, job.getSuccessCallback());
    }

    @Test
    void testGetFailCallback() {
        assertEquals(failure, job.getFailCallback());
    }
}