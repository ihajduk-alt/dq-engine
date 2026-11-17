package io.dqengine.example.http;

import io.dqengine.core.domain.RecordError;
import io.dqengine.core.domain.RecordOutcome;
import io.dqengine.core.domain.RuleFilter;
import io.dqengine.core.domain.RunSummary;
import io.dqengine.core.port.in.RecordSource;
import io.dqengine.core.port.in.RunListener;
import io.dqengine.core.port.in.ValidationEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
class ValidationController {

    // Bounds a request's memory: its records and outcomes are held until the response. Larger inputs belong in a worker
    static final int MAX_RECORDS = 1_000;

    private final ValidationEngine engine;

    @PostMapping("/v1/validations")
    ValidationResponse validate(@RequestBody List<Map<String, Object>> records,
                                @RequestParam(name = "category", required = false) List<String> categories,
                                @RequestParam(name = "countryScope", defaultValue = "false") boolean countryScope) {
        // Spring has read the whole body by now; a limit in bytes belongs to the gateway
        if (records.size() > MAX_RECORDS) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE,
                    "at most " + MAX_RECORDS + " records per request");
        }
        RuleFilter filter = RuleFilter.all();
        if (categories != null) {
            filter = filter.and(RuleFilter.category(categories.toArray(String[]::new)));
        }
        if (countryScope) {
            filter = filter.and(RuleFilter.countryScope());
        }
        List<ValidationResponse.Outcome> outcomes = new ArrayList<>(records.size());
        // One batch: the response is the unit of commit
        RunSummary summary = engine.select(filter).validateAll(RecordSource.of(records), MAX_RECORDS,
                new RunListener() {
                    @Override
                    public void outcome(long position, RecordOutcome outcome) {
                        outcomes.add(ValidationResponse.Outcome.of(position, outcome));
                    }

                    @Override
                    public void recordError(RecordError error) {
                        outcomes.add(ValidationResponse.Outcome.of(error));
                    }
                });
        return new ValidationResponse(outcomes, summary);
    }
}
