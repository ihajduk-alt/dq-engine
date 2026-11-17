package io.dqengine.core;

import static io.dqengine.testing.FixtureRecords.R1;
import static io.dqengine.testing.FixtureRecords.R2;
import static io.dqengine.testing.FixtureRecords.R3;
import static io.dqengine.testing.FixtureRecords.R4_NUMERIC_IBAN;
import static org.assertj.core.api.Assertions.assertThat;

import io.dqengine.core.domain.Decision;
import io.dqengine.core.domain.FieldRead;
import io.dqengine.core.port.in.ValidationEngine;
import io.dqengine.testing.FixtureRules;
import io.dqengine.testing.ValidatedBatch;
import java.util.List;
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
}
