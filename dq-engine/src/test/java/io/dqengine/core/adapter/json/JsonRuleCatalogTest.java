package io.dqengine.core.adapter.json;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dqengine.core.domain.Rule;
import io.dqengine.core.port.in.ValidationEngine;
import io.dqengine.core.port.out.RuleCatalog;
import io.dqengine.core.port.out.RuleCatalogException;
import io.dqengine.testing.FixtureRecords;
import io.dqengine.testing.FixtureRules;
import io.dqengine.testing.ValidatedBatch;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class JsonRuleCatalogTest {

    @Test
    void rulesLoadedFromJsonGiveTheSameResultsAsTheJavaRules() throws IOException {
        RuleCatalog fromJson;
        try (InputStream in = getClass().getResourceAsStream(FixtureRules.JSON_RESOURCE)) {
            fromJson = JsonRuleCatalog.read(in, FixtureRules.namedLogic());
        }

        assertThat(ValidatedBatch.of(ValidationEngine.create(fromJson), FixtureRecords.all()).results())
                .hasSize(9)
                .isEqualTo(ValidatedBatch.of(ValidationEngine.create(FixtureRules.catalog()), FixtureRecords.all())
                        .results());
    }

    @Test
    void unknownPropertiesAreIgnoredSoTheRulesServiceCanAddFields() {
        String withVersion = """
                [{"id": "r", "label": "L", "severity": "INFO", "logic": "x", "version": 3,
                  "mapping": {"default": "VALID"}}]""";

        RuleCatalog catalog = JsonRuleCatalog.read(new ByteArrayInputStream(withVersion.getBytes(UTF_8)),
                (ruleId, logic) -> f -> logic);

        assertThat(catalog.rules()).extracting(Rule::id).containsExactly("r");
    }

    // Each kind of invalid definition (bad enum value, invalid logic) could get its own test.
    @Test
    void invalidDefinitionFailsTheLoad() {
        String noDefault = """
                [{"id": "r", "label": "L", "severity": "INFO", "logic": "x", "mapping": {"ok": "VALID"}}]""";

        assertThatThrownBy(() -> JsonRuleCatalog.read(new ByteArrayInputStream(noDefault.getBytes(UTF_8)),
                (ruleId, logic) -> f -> logic))
                .isInstanceOf(RuleCatalogException.class)
                .hasMessageContaining("defaultDecision");
    }
}
