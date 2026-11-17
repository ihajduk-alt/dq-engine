package io.dqengine.core.domain;

// The outcome of one rule on one record
public record ValidationResult(Decision decision, Severity severity, Provenance provenance) {
}
