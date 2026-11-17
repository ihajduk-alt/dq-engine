package io.dqengine.core.domain;

// A record could not be read, so no rule ran on it. Reported by the host, not the engine
public record RecordError(long position, String message, Throwable cause) {
}
