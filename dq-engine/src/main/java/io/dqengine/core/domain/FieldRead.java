package io.dqengine.core.domain;

import java.util.Optional;

// A field a rule read and the value it saw; empty if the field was absent
public record FieldRead(String field, Optional<Object> value) {
}
