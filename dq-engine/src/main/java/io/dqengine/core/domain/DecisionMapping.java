package io.dqengine.core.domain;

import java.util.Map;
import java.util.Objects;

// Turns a rule's computed value into a Decision: a table of values, plus a default for anything the table doesn't list.
public record DecisionMapping(Map<String, Decision> entries, Decision defaultDecision) {

    public DecisionMapping {
        entries = Map.copyOf(entries);
        Objects.requireNonNull(defaultDecision, "defaultDecision");
    }

    public Decision decide(String computedValue) {
        return entries.getOrDefault(computedValue, defaultDecision);
    }
}
