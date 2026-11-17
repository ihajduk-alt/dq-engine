package io.dqengine.core.domain;

@FunctionalInterface
public interface RuleLogic {

    String process(RecordFields fields);
}
