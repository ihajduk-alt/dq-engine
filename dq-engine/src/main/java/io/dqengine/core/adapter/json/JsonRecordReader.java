package io.dqengine.core.adapter.json;

import io.dqengine.core.domain.InvalidRecordException;
import io.dqengine.core.domain.PartnerRecord;
import io.dqengine.core.port.in.RecordSource;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

public final class JsonRecordReader implements RecordSource, AutoCloseable {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
            .build();
    private static final TypeReference<Map<String, Object>> FIELDS = new TypeReference<>() {
    };

    private final InputStream json;
    private MappingIterator<JsonNode> elements;

    private JsonRecordReader(InputStream json) {
        this.json = json;
    }

    public static JsonRecordReader ofArray(InputStream json) {
        return new JsonRecordReader(json);
    }

    @Override
    public Optional<PartnerRecord> next() {
        JsonNode element;
        try {
            if (elements == null) {
                elements = MAPPER.readerFor(JsonNode.class).readValues(json);
            }
            if (!elements.hasNextValue()) {
                return Optional.empty();
            }
            element = elements.nextValue();
        } catch (JacksonException e) {
            // One error for all broken input; truncation or content after the array could get their own messages.
            throw new JsonInputException("malformed JSON: " + e.getOriginalMessage(), e);
        }
        if (!(element instanceof ObjectNode)) {
            throw new InvalidRecordException("element is not a JSON object");
        }
        return Optional.of(new PartnerRecord(MAPPER.treeToValue(element, FIELDS)));
    }

    @Override
    public void close() {
        if (elements != null) {
            elements.close();
        }
    }
}
