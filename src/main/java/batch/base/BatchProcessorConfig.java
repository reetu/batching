package batch.base;

import batch.ProcessorConfig;

public record BatchProcessorConfig(long batchInterval, int batchSize, long pollInterval) implements ProcessorConfig {}
