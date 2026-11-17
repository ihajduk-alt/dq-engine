package io.dqengine.core.port.in;

import io.dqengine.core.domain.PartnerRecord;
import io.dqengine.core.domain.Provenance;
import io.dqengine.core.domain.RecordFields;
import io.dqengine.core.domain.RecordOutcome;
import io.dqengine.core.domain.Rule;
import io.dqengine.core.domain.ValidationResult;
import io.dqengine.core.port.out.RuleCatalog;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

// Package-private, so hosts can't reach it on the classpath either
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class DefaultValidationEngine implements ValidationEngine {

    private final List<Rule> rules;

    static DefaultValidationEngine of(RuleCatalog catalog) {
        return new DefaultValidationEngine(List.copyOf(catalog.rules()));
    }

    @Override
    public RecordOutcome validate(PartnerRecord record) {
        List<ValidationResult> results = new ArrayList<>();
        for (Rule rule : rules) {
            results.add(evaluate(rule, record));
        }
        return new RecordOutcome(record.id(), results, List.of());
    }

    private static ValidationResult evaluate(Rule rule, PartnerRecord record) {
        RecordFields fields = new RecordFields(record);
        String computedValue = rule.logic().process(fields);
        return new ValidationResult(rule.mapping().decide(computedValue), rule.severity(),
                new Provenance(rule.id(), record.id(), fields.reads(), computedValue));
    }
}
