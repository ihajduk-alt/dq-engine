package io.dqengine.testing;

import io.dqengine.core.domain.PartnerRecord;
import java.util.List;
import java.util.Map;

public final class FixtureRecords {

    public static final PartnerRecord R1 = new PartnerRecord(Map.of(
            "id", "r1", "vatId", "DE111111111", "country", "DE", "legalName", "ACME GmbH", "iban", "DE123456789"));

    public static final PartnerRecord R2 = new PartnerRecord(Map.of(
            "id", "r2", "vatId", "FR22", "country", "FR", "legalName", "Bricolage SARL", "iban", "FR123456789"));

    public static final PartnerRecord R3 = new PartnerRecord(Map.of(
            "id", "r3", "country", "ZZ", "legalName", "Nowhere Ltd", "iban", ""));

    public static final PartnerRecord R4_NUMERIC_IBAN = new PartnerRecord(Map.of(
            "id", "r4", "vatId", "DE444444444", "country", "DE", "legalName", "Broken AG", "iban", 12345));

    private FixtureRecords() {
    }

    public static List<PartnerRecord> all() {
        return List.of(R1, R2, R3);
    }
}
