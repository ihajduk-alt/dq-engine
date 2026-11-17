package io.dqengine.core.port.out;

import io.dqengine.core.domain.Rule;
import java.util.List;

@FunctionalInterface
public interface RuleCatalog {

    List<Rule> rules();

    static RuleCatalog of(Rule... rules) {
        List<Rule> copy = List.of(rules);
        return () -> copy;
    }
}
