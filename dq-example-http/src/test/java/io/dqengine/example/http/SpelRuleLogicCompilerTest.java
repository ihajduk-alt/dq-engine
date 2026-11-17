package io.dqengine.example.http;

import static org.assertj.core.api.Assertions.assertThat;

import io.dqengine.core.adapter.json.JsonRuleCatalog;
import io.dqengine.core.domain.Decision;
import io.dqengine.core.domain.DecisionMapping;
import io.dqengine.core.domain.RecordOutcome;
import io.dqengine.core.domain.Rule;
import io.dqengine.core.domain.RuleStatus;
import io.dqengine.core.domain.Severity;
import io.dqengine.core.port.in.ValidationEngine;
import io.dqengine.core.port.out.RuleCatalog;
import io.dqengine.testing.FixtureRecords;
import io.dqengine.testing.FixtureRules;
import io.dqengine.testing.ValidatedBatch;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SpelRuleLogicCompilerTest {

    private final SpelRuleLogicCompiler compiler = new SpelRuleLogicCompiler();

    @Test
    void bundledSpelRulesGiveTheResultsOfTheJavaRules() throws IOException {
        RuleCatalog spel;
        try (InputStream in = getClass().getResourceAsStream("/rules.json")) {
            spel = JsonRuleCatalog.read(in, compiler);
        }

        assertThat(run(spel).results()).hasSize(9).isEqualTo(run(FixtureRules.catalog()).results());
    }

    // Invalid syntax and calls from many threads could get their own tests.
    @ParameterizedTest
    @ValueSource(strings = {
            "T(java.lang.String)",
            "new java.lang.String('x')",
            "vatId.matches('DE.*') ? 'ok' : 'bad'"})
    void expressionsThatReachBeyondTheRecordFailAsRuleErrors(String expression) {
        Rule rule = Rule.builder().id("sandbox").label("sandbox").status(RuleStatus.RELEASED).severity(Severity.INFO)
                .logic(compiler.compile("sandbox", expression))
                .mapping(new DecisionMapping(Map.of(), Decision.REVIEW))
                .build();

        RecordOutcome outcome = ValidationEngine.create(RuleCatalog.of(rule)).validate(FixtureRecords.R1);

        assertThat(outcome.results()).isEmpty();
        assertThat(outcome.errors()).hasSize(1);
    }

    private static ValidatedBatch run(RuleCatalog catalog) {
        return ValidatedBatch.of(ValidationEngine.create(catalog), FixtureRecords.all());
    }
}
