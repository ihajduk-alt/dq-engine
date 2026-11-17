package io.dqengine.example.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.dqengine.core.domain.FieldRead;
import io.dqengine.core.domain.RecordError;
import io.dqengine.core.domain.RecordOutcome;
import io.dqengine.core.domain.RuleError;
import io.dqengine.core.domain.RunSummary;
import io.dqengine.core.domain.ValidationResult;
import java.util.List;

record ValidationResponse(List<Outcome> outcomes, RunSummary summary) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Outcome(long position, String recordId, List<ValidationResult> results, List<Error> errors) {

        static Outcome of(long position, RecordOutcome outcome) {
            return new Outcome(position, outcome.recordId(), outcome.results(),
                    outcome.errors().stream().map(Error::of).toList());
        }

        static Outcome of(RecordError error) {
            return new Outcome(error.position(), null, List.of(), List.of(Error.of(error)));
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Error(String type, String ruleId, String message, List<FieldRead> fieldsRead) {

        static Error of(RuleError e) {
            return new Error("RULE", e.ruleId(), e.message(), e.fieldsRead());
        }

        static Error of(RecordError e) {
            return new Error("RECORD", null, e.message(), null);
        }
    }
}
