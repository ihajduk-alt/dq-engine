package io.dqengine.core;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import io.dqengine.core.adapter.json.JsonRecordReader;
import io.dqengine.core.domain.RecordError;
import io.dqengine.core.domain.RecordOutcome;
import io.dqengine.core.domain.RunSummary;
import io.dqengine.core.port.in.RunListener;
import io.dqengine.core.port.in.ValidationEngine;
import io.dqengine.testing.FixtureRules;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

// The whole path a batch worker takes: JSON in, one record at a time, outcomes out in batches
@Tag("scale")
class ScaleTest {

    private static final long RECORDS = 1_000_000;
    private static final int BATCH_SIZE = 10_000;

    @Test
    void aMillionJsonRecordsRunInA64MegabyteHeap() {
        AtomicLong batches = new AtomicLong();
        RunSummary summary;
        try (JsonRecordReader records = JsonRecordReader.ofArray(json(RECORDS))) {
            summary = ValidationEngine.create(FixtureRules.catalog()).validateAll(records, BATCH_SIZE,
                    new RunListener() {
                        @Override
                        public void outcome(long position, RecordOutcome outcome) {
                        }

                        @Override
                        public void recordError(RecordError error) {
                        }

                        @Override
                        public void batchEnd(RunSummary soFar) {
                            batches.incrementAndGet();
                        }
                    });
        }

        assertThat(Runtime.getRuntime().maxMemory()).isLessThanOrEqualTo(64L * 1024 * 1024);
        assertThat(summary.records()).isEqualTo(RECORDS);
        assertThat(summary.results()).isEqualTo(3 * RECORDS);
        assertThat(summary.errors()).isZero();
        assertThat(batches).hasValue(RECORDS / BATCH_SIZE);
    }

    // A JSON array generated as it is read, so the input never sits in memory either
    private static InputStream json(long count) {
        Iterator<String> parts = Stream.concat(
                LongStream.range(0, count).mapToObj(i -> (i == 0 ? "[" : ",") + record(i)),
                Stream.of("]")).iterator();
        return new SequenceInputStream(new Enumeration<>() {
            @Override
            public boolean hasMoreElements() {
                return parts.hasNext();
            }

            @Override
            public InputStream nextElement() {
                return new ByteArrayInputStream(parts.next().getBytes(UTF_8));
            }
        });
    }

    private static String record(long i) {
        return switch ((int) (i % 3)) {
            case 0 -> "{\"id\":\"s%d\",\"country\":\"DE\",\"vatId\":\"DE%d\",\"iban\":\"DE%d\"}".formatted(i, i, i);
            case 1 -> "{\"id\":\"s%d\",\"country\":\"FR\",\"vatId\":\"FR22\",\"iban\":\"FR%d\"}".formatted(i, i);
            default -> "{\"id\":\"s%d\",\"country\":\"ZZ\",\"iban\":\"\"}".formatted(i);
        };
    }
}
