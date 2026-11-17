package io.dqengine.core.port.out;

import io.dqengine.core.domain.RuleLogic;

@FunctionalInterface
public interface RuleLogicCompiler {

    RuleLogic compile(String ruleId, String logic);
}
