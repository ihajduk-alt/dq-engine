package io.dqengine.core.port.in;

import io.dqengine.core.domain.PartnerRecord;
import io.dqengine.core.domain.RecordOutcome;
import io.dqengine.core.domain.RuleFilter;
import io.dqengine.core.domain.RunSummary;
import io.dqengine.core.port.out.RuleCatalog;

public interface ValidationEngine {

    static ValidationEngine create(RuleCatalog catalog) {
        return DefaultValidationEngine.of(catalog);
    }

    ValidationEngine select(RuleFilter filter);

    RecordOutcome validate(PartnerRecord record);

    RunSummary validateAll(RecordSource records, int batchSize, RunListener listener);
}
