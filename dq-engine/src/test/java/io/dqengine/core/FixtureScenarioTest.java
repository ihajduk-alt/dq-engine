package io.dqengine.core;

import static io.dqengine.core.domain.Decision.INVALID;
import static io.dqengine.core.domain.Decision.NOT_APPLICABLE;
import static io.dqengine.core.domain.Decision.REVIEW;
import static io.dqengine.core.domain.Decision.VALID;
import static io.dqengine.core.domain.Severity.ERROR;
import static io.dqengine.core.domain.Severity.INFO;
import static io.dqengine.core.domain.Severity.WARNING;
import static org.assertj.core.api.Assertions.assertThat;

import io.dqengine.core.domain.Decision;
import io.dqengine.core.domain.FieldRead;
import io.dqengine.core.domain.Provenance;
import io.dqengine.core.domain.RunSummary;
import io.dqengine.core.domain.ValidationResult;
import io.dqengine.core.port.in.ValidationEngine;
import io.dqengine.testing.FixtureRecords;
import io.dqengine.testing.FixtureRules;
import io.dqengine.testing.ValidatedBatch;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FixtureScenarioTest {

    private static final ValidatedBatch BATCH =
            ValidatedBatch.of(ValidationEngine.create(FixtureRules.catalog()), FixtureRecords.all());

    @ParameterizedTest(name = "{0} / {1} -> {2}")
    @CsvSource({
            "r1, countryBlocked, NOT_APPLICABLE",
            "r1, vatFormat,      VALID",
            "r1, ibanFormat,     VALID",
            "r2, countryBlocked, NOT_APPLICABLE",
            "r2, vatFormat,      INVALID",
            "r2, ibanFormat,     VALID",
            "r3, countryBlocked, INVALID",
            "r3, vatFormat,      INVALID",
            "r3, ibanFormat,     INVALID",
    })
    void expectedDecision(String recordId, String ruleId, Decision expected) {
        assertThat(BATCH.decision(recordId, ruleId)).contains(expected);
    }

    @Test
    void resultCarriesSeverityAndTheFieldsTheRuleRead() {
        assertThat(BATCH.result("r3", "vatFormat")).contains(new ValidationResult(INVALID, ERROR,
                new Provenance("vatFormat", "r3", List.of(new FieldRead("vatId", Optional.empty())), "bad")));
    }

    @Test
    void summary() {
        assertThat(BATCH.summary()).isEqualTo(new RunSummary(3, 9, 0,
                Map.of(VALID, 3L, INVALID, 4L, REVIEW, 0L, NOT_APPLICABLE, 2L),
                Map.of(ERROR, 6L, WARNING, 3L, INFO, 0L)));
    }
}
