package io.dqengine.core.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * What rule logic sees of a record. Every read is recorded, absent fields included, and ends up in the
 * result's provenance. A {@code null} field counts as absent; an empty string counts as present.
 */
@RequiredArgsConstructor
public final class RecordFields {

    private final PartnerRecord record;
    private final Map<String, FieldRead> reads = new LinkedHashMap<>();

    public Optional<Object> value(String field) {
        Optional<Object> value = Optional.ofNullable(record.fields().get(field));
        reads.putIfAbsent(field, new FieldRead(field, value));
        return value;
    }

    public Optional<String> text(String field) {
        return value(field).map(String.class::cast);
    }

    public List<FieldRead> reads() {
        return List.copyOf(reads.values());
    }
}
