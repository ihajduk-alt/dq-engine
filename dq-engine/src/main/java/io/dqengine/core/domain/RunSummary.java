package io.dqengine.core.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public record RunSummary(long records, long results, long errors, Map<Decision, Long> byDecision,
                         Map<Severity, Long> bySeverity) {

    public RunSummary {
        byDecision = counts(Decision.class, byDecision);
        bySeverity = counts(Severity.class, bySeverity);
    }

    public static RunSummary empty() {
        return new RunSummary(0, 0, 0, Map.of(), Map.of());
    }

    public RunSummary plus(RecordOutcome outcome) {
        Map<Decision, Long> decisions = new HashMap<>(byDecision);
        Map<Severity, Long> severities = new HashMap<>(bySeverity);
        for (ValidationResult result : outcome.results()) {
            decisions.merge(result.decision(), 1L, Long::sum);
            severities.merge(result.severity(), 1L, Long::sum);
        }
        return new RunSummary(records + 1, results + outcome.results().size(), errors + outcome.errors().size(),
                decisions, severities);
    }

    // An unmodifiable copy in enum order, with a zero for every missing constant
    private static <K extends Enum<K>> Map<K, Long> counts(Class<K> type, Map<K, Long> counts) {
        Map<K, Long> copy = new EnumMap<>(type);
        for (K key : type.getEnumConstants()) {
            copy.put(key, counts.getOrDefault(key, 0L));
        }
        return Collections.unmodifiableMap(copy);
    }
}
