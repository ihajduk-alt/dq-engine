package io.dqengine.testing;

import static io.dqengine.core.domain.Decision.INVALID;
import static io.dqengine.core.domain.Decision.NOT_APPLICABLE;
import static io.dqengine.core.domain.Decision.REVIEW;
import static io.dqengine.core.domain.Decision.VALID;

import io.dqengine.core.domain.DecisionMapping;
import io.dqengine.core.domain.Rule;
import io.dqengine.core.domain.RuleLogic;
import io.dqengine.core.domain.RuleStatus;
import io.dqengine.core.domain.Severity;
import io.dqengine.core.port.out.RuleCatalog;
import io.dqengine.core.port.out.RuleLogicCompiler;
import java.util.Map;
import java.util.Set;

public final class FixtureRules {

    public static final String JSON_RESOURCE = "/fixtures/rules.json";

    private static final Map<String, RuleLogic> LOGIC = Map.of(
            "countryBlocked", f -> f.text("country").filter("ZZ"::equals).map(c -> "blocked").orElse("ok"),
            "vatFormat", f -> f.text("vatId").filter(vat -> vat.length() >= 9).map(vat -> "ok").orElse("bad"),
            "ibanFormat", f -> f.text("iban")
                    .flatMap(iban -> f.text("country").filter(iban::startsWith))
                    .map(country -> "ok")
                    .orElse("bad"));

    public static final Rule COUNTRY_BLOCKED = Rule.builder()
            .id("countryBlocked")
            .label("Country is not blocked")
            .status(RuleStatus.RELEASED)
            .severity(Severity.ERROR)
            .categories(Set.of("compliance"))
            .logic(LOGIC.get("countryBlocked"))
            .mapping(new DecisionMapping(Map.of("blocked", INVALID), NOT_APPLICABLE))
            .build();

    public static final Rule VAT_FORMAT = Rule.builder()
            .id("vatFormat")
            .label("VAT id is present and at least 9 characters long")
            .status(RuleStatus.RELEASED)
            .severity(Severity.ERROR)
            .categories(Set.of("tax"))
            .logic(LOGIC.get("vatFormat"))
            .mapping(new DecisionMapping(Map.of("ok", VALID, "bad", INVALID), REVIEW))
            .build();

    public static final Rule IBAN_FORMAT = Rule.builder()
            .id("ibanFormat")
            .label("IBAN is present and starts with the record's country")
            .status(RuleStatus.RELEASED)
            .severity(Severity.WARNING)
            .categories(Set.of("banking"))
            .logic(LOGIC.get("ibanFormat"))
            .mapping(new DecisionMapping(Map.of("ok", VALID, "bad", INVALID), REVIEW))
            .build();

    private FixtureRules() {
    }

    public static RuleCatalog catalog() {
        return RuleCatalog.of(COUNTRY_BLOCKED, VAT_FORMAT, IBAN_FORMAT);
    }

    public static RuleLogicCompiler namedLogic() {
        return (ruleId, name) -> LOGIC.get(name);
    }
}
