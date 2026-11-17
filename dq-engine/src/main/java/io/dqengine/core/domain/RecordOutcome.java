package io.dqengine.core.domain;

import java.util.List;

// Everything the engine produced for one record: results and rule errors, both in catalog order
public record RecordOutcome(String recordId, List<ValidationResult> results, List<RuleError> errors) {

    public RecordOutcome {
        results = List.copyOf(results);
        errors = List.copyOf(errors);
    }
}
