package io.dqengine.core;

import static io.dqengine.testing.FixtureRules.COUNTRY_BLOCKED;
import static io.dqengine.testing.FixtureRules.IBAN_FORMAT;
import static io.dqengine.testing.FixtureRules.VAT_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dqengine.core.domain.Decision;
import io.dqengine.core.domain.DecisionMapping;
import io.dqengine.core.domain.Rule;
import io.dqengine.core.domain.RuleFilter;
import io.dqengine.core.domain.RuleStatus;
import io.dqengine.core.domain.Severity;
import io.dqengine.core.port.in.ValidationEngine;
import io.dqengine.core.port.out.RuleCatalog;
import io.dqengine.core.port.out.RuleCatalogException;
import io.dqengine.testing.FixtureRecords;
import io.dqengine.testing.ValidatedBatch;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RuleSelectionTest {

    private static final Rule DRAFT = rule("draftRule").status(RuleStatus.DRAFT).build();
    private static final Rule GERMAN_IBAN = rule("germanIban").status(RuleStatus.RELEASED)
            .countryScope("DE").categories(Set.of("banking")).build();

    private static final ValidationEngine ENGINE = ValidationEngine.create(RuleCatalog.of(
            COUNTRY_BLOCKED, VAT_FORMAT, IBAN_FORMAT, DRAFT, GERMAN_IBAN));

    // Each filter on its own, and filters that throw, could get their own tests.
    @Test
    void draftRulesNeverRunEvenWhenSelected() {
        assertThat(run(RuleFilter.status(RuleStatus.DRAFT))).isEmpty();
    }

    @Test
    void categoryAndCountryScopeCombine() {
        assertThat(run(RuleFilter.category("banking").and(RuleFilter.countryScope())))
                .containsExactly("r1/ibanFormat", "r1/germanIban", "r2/ibanFormat", "r3/ibanFormat");
    }

    @Test
    void ruleOnlyFiltersRunOncePerRuleNotOncePerRecord() {
        AtomicInteger calls = new AtomicInteger();
        ValidationEngine selected = ENGINE.select(RuleFilter.of(rule -> calls.incrementAndGet() > 0));

        FixtureRecords.all().forEach(selected::validate);

        assertThat(calls).hasValue(4);
    }

    @Test
    void onlyReleasedRulesNeedUniqueIds() {
        Rule nextVatFormat = rule("vatFormat").status(RuleStatus.DRAFT).build();

        assertThatCode(() -> ValidationEngine.create(RuleCatalog.of(VAT_FORMAT, nextVatFormat)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ValidationEngine.create(RuleCatalog.of(VAT_FORMAT, VAT_FORMAT)))
                .isInstanceOf(RuleCatalogException.class)
                .hasMessageContaining("vatFormat");
    }

    private static List<String> run(RuleFilter filter) {
        return ValidatedBatch.of(ENGINE, filter, FixtureRecords.all()).results().stream()
                .map(r -> r.provenance().recordId() + "/" + r.provenance().ruleId())
                .toList();
    }

    private static Rule.RuleBuilder rule(String id) {
        return Rule.builder().id(id).label(id).severity(Severity.INFO)
                .logic(f -> f.text("iban").map(iban -> iban.length() == 22 ? "ok" : "bad").orElse("bad"))
                .mapping(new DecisionMapping(Map.of("ok", Decision.VALID, "bad", Decision.INVALID), Decision.REVIEW));
    }
}
