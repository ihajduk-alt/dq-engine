package io.dqengine.core.adapter.json;

import io.dqengine.core.domain.Decision;
import io.dqengine.core.domain.DecisionMapping;
import io.dqengine.core.domain.Rule;
import io.dqengine.core.domain.RuleStatus;
import io.dqengine.core.domain.Severity;
import io.dqengine.core.port.out.RuleCatalog;
import io.dqengine.core.port.out.RuleCatalogException;
import io.dqengine.core.port.out.RuleLogicCompiler;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

public final class JsonRuleCatalog {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
            // Tolerant reader: the rules service can add a property before every engine knows it.
            // Catching typos in hand-written rules is the authoring side's job, e.g. a schema check on save.
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    record Definition(String id, String label, RuleStatus status, Severity severity, String countryScope,
                      Set<String> categories, String logic, Map<String, Decision> mapping) {
    }

    private JsonRuleCatalog() {
    }

    /** Reads the whole catalog. Doesn't close {@code json}. */
    public static RuleCatalog read(InputStream json, RuleLogicCompiler compiler) {
        try {
            List<Rule> rules = MAPPER.readValue(json, new TypeReference<List<Definition>>() {
            }).stream().map(definition -> toRule(definition, compiler)).toList();
            return () -> rules;
        } catch (RuntimeException e) {
            // One error for any invalid definition. Naming the rule and property at fault would help catalog authors.
            throw new RuleCatalogException("invalid rule catalog: " + e.getMessage(), e);
        }
    }

    private static Rule toRule(Definition definition, RuleLogicCompiler compiler) {
        Map<String, Decision> mapping = new HashMap<>(definition.mapping());
        Decision defaultDecision = mapping.remove("default");
        return Rule.builder()
                .id(definition.id())
                .label(definition.label())
                .status(definition.status())
                .severity(definition.severity())
                .countryScope(definition.countryScope())
                .categories(definition.categories())
                .logic(compiler.compile(definition.id(), definition.logic()))
                .mapping(new DecisionMapping(mapping, defaultDecision))
                .build();
    }
}
