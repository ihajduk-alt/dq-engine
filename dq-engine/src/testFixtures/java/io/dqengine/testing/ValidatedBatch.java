package io.dqengine.testing;

import io.dqengine.core.domain.Decision;
import io.dqengine.core.domain.PartnerRecord;
import io.dqengine.core.domain.RecordOutcome;
import io.dqengine.core.domain.RuleError;
import io.dqengine.core.domain.RuleFilter;
import io.dqengine.core.domain.RunSummary;
import io.dqengine.core.domain.ValidationResult;
import io.dqengine.core.port.in.ValidationEngine;
import java.util.List;
import java.util.Optional;

// A batch validated record by record, kept whole for assertions
public record ValidatedBatch(List<RecordOutcome> outcomes, RunSummary summary) {

    public static ValidatedBatch of(ValidationEngine engine, List<PartnerRecord> records) {
        return of(engine, RuleFilter.all(), records);
    }

    public static ValidatedBatch of(ValidationEngine engine, RuleFilter filter, List<PartnerRecord> records) {
        ValidationEngine selected = engine.select(filter);
        List<RecordOutcome> outcomes = records.stream().map(selected::validate).toList();
        RunSummary summary = RunSummary.empty();
        for (RecordOutcome outcome : outcomes) {
            summary = summary.plus(outcome);
        }
        return new ValidatedBatch(outcomes, summary);
    }

    public List<ValidationResult> results() {
        return outcomes.stream().flatMap(outcome -> outcome.results().stream()).toList();
    }

    public List<RuleError> errors() {
        return outcomes.stream().flatMap(outcome -> outcome.errors().stream()).toList();
    }

    public Optional<ValidationResult> result(String recordId, String ruleId) {
        return results().stream()
                .filter(r -> r.provenance().recordId().equals(recordId) && r.provenance().ruleId().equals(ruleId))
                .findFirst();
    }

    public Optional<Decision> decision(String recordId, String ruleId) {
        return result(recordId, ruleId).map(ValidationResult::decision);
    }
}
