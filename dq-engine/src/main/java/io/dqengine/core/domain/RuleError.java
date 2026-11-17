package io.dqengine.core.domain;

import java.util.List;

public record RuleError(String ruleId, String recordId, String message, List<FieldRead> fieldsRead,
                        Throwable cause) {

    public RuleError {
        fieldsRead = List.copyOf(fieldsRead);
    }
}
