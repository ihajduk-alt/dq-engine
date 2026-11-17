package io.dqengine.testing;

import io.dqengine.core.domain.Decision;
import io.dqengine.core.domain.PartnerRecord;
import io.dqengine.core.domain.RecordError;
import io.dqengine.core.domain.RecordOutcome;
import io.dqengine.core.domain.RuleError;
import io.dqengine.core.domain.RuleFilter;
import io.dqengine.core.domain.RunSummary;
import io.dqengine.core.domain.ValidationResult;
import io.dqengine.core.port.in.RecordSource;
import io.dqengine.core.port.in.RunListener;
import io.dqengine.core.port.in.ValidationEngine;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

// A run through validateAll, kept whole for assertions
public record ValidatedBatch(List<RecordOutcome> outcomes, List<RecordError> recordErrors, RunSummary summary) {

    public static ValidatedBatch of(ValidationEngine engine, List<PartnerRecord> records) {
        return of(engine, RuleFilter.all(), records);
    }

    public static ValidatedBatch of(ValidationEngine engine, RuleFilter filter, List<PartnerRecord> records) {
        Iterator<PartnerRecord> iterator = records.iterator();
        return run(engine.select(filter), () -> iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty());
    }

    public static ValidatedBatch run(ValidationEngine engine, RecordSource records) {
        List<RecordOutcome> outcomes = new ArrayList<>();
        List<RecordError> recordErrors = new ArrayList<>();
        RunSummary summary = engine.validateAll(records, 100, new RunListener() {
            @Override
            public void outcome(long position, RecordOutcome outcome) {
                outcomes.add(outcome);
            }

            @Override
            public void recordError(RecordError error) {
                recordErrors.add(error);
            }
        });
        return new ValidatedBatch(outcomes, recordErrors, summary);
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
