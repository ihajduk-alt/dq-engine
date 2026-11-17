package io.dqengine.core.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

// One business partner: a flat map of fields with a non-blank string id. A null field is treated as missing
public record PartnerRecord(Map<String, Object> fields) {

    public PartnerRecord {
        if (fields == null || !(fields.get("id") instanceof String id) || id.isBlank()) {
            throw new InvalidRecordException("record has no 'id' (a non-blank string)");
        }
        Map<String, Object> copy = new LinkedHashMap<>(fields);
        copy.values().removeIf(Objects::isNull);
        fields = Collections.unmodifiableMap(copy);
    }

    public String id() {
        return (String) fields.get("id");
    }
}
