package io.dqengine.core.port.in;

import io.dqengine.core.domain.PartnerRecord;
import io.dqengine.core.domain.Provenance;
import io.dqengine.core.domain.RecordFields;
import io.dqengine.core.domain.RecordOutcome;
import io.dqengine.core.domain.Rule;
import io.dqengine.core.domain.RuleError;
import io.dqengine.core.domain.RuleFilter;
import io.dqengine.core.domain.RuleStatus;
import io.dqengine.core.domain.ValidationResult;
import io.dqengine.core.port.out.RuleCatalog;
import io.dqengine.core.port.out.RuleCatalogException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

// Package-private, so hosts can't reach it on the classpath either
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class DefaultValidationEngine implements ValidationEngine {

    private final List<Rule> rules;
    private final BiPredicate<Rule, PartnerRecord> appliesTo;

    static DefaultValidationEngine of(RuleCatalog catalog) {
        List<Rule> released = catalog.rules().stream().filter(rule -> rule.status() == RuleStatus.RELEASED).toList();
        Set<String> ids = new HashSet<>();
        List<String> duplicates = released.stream().map(Rule::id).filter(id -> !ids.add(id)).distinct().toList();
        if (!duplicates.isEmpty()) {
            throw new RuleCatalogException("released rules share an id: " + duplicates);
        }
        return new DefaultValidationEngine(released, (rule, record) -> true);
    }

    @Override
    public ValidationEngine select(RuleFilter filter) {
        return new DefaultValidationEngine(rules.stream().filter(filter.selects()).toList(),
                appliesTo.and(filter.appliesTo()));
    }

    // Country scope is checked per selected rule; indexing the rules by country would make it a lookup.
    @Override
    public RecordOutcome validate(PartnerRecord record) {
        List<ValidationResult> results = new ArrayList<>();
        List<RuleError> errors = new ArrayList<>();
        for (Rule rule : rules) {
            if (appliesTo.test(rule, record)) {
                evaluate(rule, record, results, errors);
            }
        }
        return new RecordOutcome(record.id(), results, errors);
    }

    private static void evaluate(Rule rule, PartnerRecord record, List<ValidationResult> results,
                                 List<RuleError> errors) {
        RecordFields fields = new RecordFields(record);
        try {
            String computedValue = rule.logic().process(fields);
            Objects.requireNonNull(computedValue, "logic returned null");
            results.add(new ValidationResult(rule.mapping().decide(computedValue), rule.severity(),
                    new Provenance(rule.id(), record.id(), fields.reads(), computedValue)));
        } catch (Exception e) {
            errors.add(new RuleError(rule.id(), record.id(), e.getMessage(), fields.reads(), e));
        }
    }
}
