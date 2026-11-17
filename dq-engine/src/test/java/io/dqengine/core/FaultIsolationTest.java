package io.dqengine.core;

import static io.dqengine.testing.FixtureRecords.R1;
import static io.dqengine.testing.FixtureRecords.R2;
import static io.dqengine.testing.FixtureRecords.R3;
import static io.dqengine.testing.FixtureRecords.R4_NUMERIC_IBAN;
import static org.assertj.core.api.Assertions.assertThat;

import io.dqengine.core.domain.Decision;
import io.dqengine.core.domain.FieldRead;
import io.dqengine.core.domain.RecordError;
import io.dqengine.core.domain.RecordOutcome;
import io.dqengine.core.port.in.RecordSource;
import io.dqengine.core.port.in.ValidationEngine;
import io.dqengine.testing.FixtureRules;
import io.dqengine.testing.ValidatedBatch;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** F3: a failing rule never sinks the record or the batch, and the caller gets both results and errors. */
class FaultIsolationTest {

    // Other failures (logic that throws or returns null, an Error that propagates) could each get a test.
    @Test
    void numericIbanFailsOnlyIbanFormatAndTheBatchCompletes() {
        ValidatedBatch batch = ValidatedBatch.of(ValidationEngine.create(FixtureRules.catalog()),
                List.of(R1, R2, R3, R4_NUMERIC_IBAN));

        assertThat(batch.errors()).singleElement().satisfies(error -> {
            assertThat(error.ruleId()).isEqualTo("ibanFormat");
            assertThat(error.recordId()).isEqualTo("r4");
            assertThat(error.message()).contains("iban");
            assertThat(error.fieldsRead()).containsExactly(new FieldRead("iban", Optional.of(12345)));
        });
        assertThat(batch.decision("r4", "vatFormat")).contains(Decision.VALID);
        assertThat(batch.summary().results()).isEqualTo(11);
        assertThat(batch.summary().errors()).isEqualTo(1);
    }

    @Test
    void unreadableRecordIsReportedAtItsPositionAndTheRunReadsOn() {
        ValidatedBatch batch = ValidatedBatch.run(ValidationEngine.create(FixtureRules.catalog()),
                RecordSource.of(List.of(R1.fields(), Map.of("legalName", "no id"), R3.fields())));

        assertThat(batch.outcomes()).extracting(RecordOutcome::recordId).containsExactly("r1", "r3");
        assertThat(batch.recordErrors()).singleElement().extracting(RecordError::position).isEqualTo(1L);
        assertThat(batch.summary().records()).isEqualTo(3);
        assertThat(batch.summary().errors()).isEqualTo(1);
    }
}
