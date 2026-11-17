package io.dqengine.core.domain;

import java.util.List;

// Why a rule decided what it did
public record Provenance(String ruleId, String recordId, List<FieldRead> fieldsRead, String computedValue) {

    public Provenance {
        fieldsRead = List.copyOf(fieldsRead);
    }
}
