package io.dqengine.core.port.in;

import io.dqengine.core.domain.PartnerRecord;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@FunctionalInterface
public interface RecordSource {

    // Empty at the end. Throws InvalidRecordException for one unreadable record; the next call reads on.
    Optional<PartnerRecord> next();

    static RecordSource of(Iterable<? extends Map<String, Object>> records) {
        Iterator<? extends Map<String, Object>> iterator = records.iterator();
        return () -> iterator.hasNext() ? Optional.of(new PartnerRecord(iterator.next())) : Optional.empty();
    }
}
