package io.dqengine.core.port.in;

import io.dqengine.core.domain.RecordError;
import io.dqengine.core.domain.RecordOutcome;
import io.dqengine.core.domain.RunSummary;

public interface RunListener {

    void outcome(long position, RecordOutcome outcome);

    void recordError(RecordError error);

    // After every batchSize records and after the last one: the host's point to commit
    default void batchEnd(RunSummary soFar) {
    }
}
