package io.dqengine.core.domain;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

// selects is decided once per rule, when an engine is narrowed; appliesTo per record, for the selected rules only
public record RuleFilter(Predicate<Rule> selects, BiPredicate<Rule, PartnerRecord> appliesTo) {

    public static RuleFilter of(Predicate<Rule> selects) {
        return new RuleFilter(selects, (rule, record) -> true);
    }

    public static RuleFilter all() {
        return of(rule -> true);
    }

    public static RuleFilter status(RuleStatus... statuses) {
        List<RuleStatus> selected = List.of(statuses);
        return of(rule -> selected.contains(rule.status()));
    }

    public static RuleFilter category(String... categories) {
        List<String> selected = List.of(categories);
        return of(rule -> rule.categories().stream().anyMatch(selected::contains));
    }

    public static RuleFilter countryScope() {
        return new RuleFilter(rule -> true, (rule, record) -> rule.countryScope().equals(Rule.WORLD)
                || rule.countryScope().equals(record.fields().get("country")));
    }

    public RuleFilter and(RuleFilter other) {
        return new RuleFilter(selects.and(other.selects), appliesTo.and(other.appliesTo));
    }
}
