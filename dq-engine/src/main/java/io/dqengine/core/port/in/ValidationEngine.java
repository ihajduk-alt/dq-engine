package io.dqengine.core.port.in;

import io.dqengine.core.domain.PartnerRecord;
import io.dqengine.core.domain.RecordOutcome;
import io.dqengine.core.port.out.RuleCatalog;

public interface ValidationEngine {

    static ValidationEngine create(RuleCatalog catalog) {
        return DefaultValidationEngine.of(catalog);
    }

    RecordOutcome validate(PartnerRecord record);
}
