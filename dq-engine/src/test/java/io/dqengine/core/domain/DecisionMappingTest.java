package io.dqengine.core.domain;

import static io.dqengine.core.domain.Decision.INVALID;
import static io.dqengine.core.domain.Decision.REVIEW;
import static io.dqengine.core.domain.Decision.VALID;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DecisionMappingTest {

    @Test
    void mappedValuesGetTheirDecisionAndEverythingElseTheDefault() {
        DecisionMapping mapping = new DecisionMapping(Map.of("ok", VALID, "bad", INVALID), REVIEW);

        assertThat(mapping.decide("bad")).isEqualTo(INVALID);
        assertThat(mapping.decide("surprise")).isEqualTo(REVIEW);
    }
}
