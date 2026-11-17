package io.dqengine.core.adapter.json;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dqengine.core.domain.InvalidRecordException;
import io.dqengine.core.domain.PartnerRecord;
import io.dqengine.testing.FixtureRecords;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// Reading one element at a time is proven by ScaleTest, which reads a million records in a 64 MB heap.
class JsonRecordReaderTest {

    // Malformed or truncated input and stream ownership could get their own tests.
    @Test
    void fixtureJsonYieldsTheFixtureRecords() throws IOException {
        try (InputStream json = getClass().getResourceAsStream(FixtureRecords.JSON_RESOURCE);
             JsonRecordReader reader = JsonRecordReader.ofArray(json)) {
            List<PartnerRecord> records = new ArrayList<>();
            for (Optional<PartnerRecord> next = reader.next(); next.isPresent(); next = reader.next()) {
                records.add(next.get());
            }

            assertThat(records).isEqualTo(FixtureRecords.all());
        }
    }

    @Test
    void badElementIsAnInvalidRecordAndTheNextOneIsStillRead() {
        JsonRecordReader reader = JsonRecordReader.ofArray(new ByteArrayInputStream("""
                [{"id": "a"}, 42, {"legalName": "no id"}, {"id": "b"}]""".getBytes(UTF_8)));

        assertThat(reader.next()).map(PartnerRecord::id).contains("a");
        assertThatThrownBy(reader::next).isInstanceOf(InvalidRecordException.class);
        assertThatThrownBy(reader::next).isInstanceOf(InvalidRecordException.class);
        assertThat(reader.next()).map(PartnerRecord::id).contains("b");
        assertThat(reader.next()).isEmpty();
    }
}
