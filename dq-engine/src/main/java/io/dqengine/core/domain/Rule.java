package io.dqengine.core.domain;

import java.util.Objects;
import java.util.Set;
import lombok.Builder;

/**
 * A named check: {@code logic} computes a value from a record, and {@code mapping} turns that value into a
 * {@link Decision}. Status defaults to {@link RuleStatus#DRAFT}, so a rule does nothing until it is released,
 * and the country scope to {@link #WORLD}.
 */
@Builder
public record Rule(String id, String label, RuleStatus status, Severity severity, String countryScope,
                   Set<String> categories, RuleLogic logic, DecisionMapping mapping) {

    public static final String WORLD = "WORLD";

    public Rule {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(logic, "logic");
        Objects.requireNonNull(mapping, "mapping");
        status = Objects.requireNonNullElse(status, RuleStatus.DRAFT);
        countryScope = Objects.requireNonNullElse(countryScope, WORLD);
        categories = categories == null ? Set.of() : Set.copyOf(categories);
    }
}
