# dq-engine

A small, embeddable Java 21 library that validates business-partner records against a catalog of data-quality rules. Each rule computes a value from a record, and a declarative mapping turns that value into a decision. The host hands the engine one record and gets that record's results back at once, with provenance, or hands it a whole batch and gets each outcome as it is produced. A failing rule is reported as an error and never stops the other rules. Rules can be written in Java or loaded as data.

**Read [`DESIGN.md`](DESIGN.md) first.** It covers the public API, fault isolation, bounded memory, the seams for hosts, assumptions, trade-offs and what I'd do next.

| Module            | What it is                                                                                  |
|-------------------|---------------------------------------------------------------------------------------------|
| `dq-engine`       | The library: domain, ports, the validation engine and the JSON adapters (Jackson 3).        |
| `dq-example-http` | An example host: a small HTTP API built on the library (Spring Boot 4, rules in SpEL).      |

## Prerequisites

JDK 21. The Gradle wrapper downloads Gradle itself. Lombok is a compile-only dependency; IntelliJ IDEA supports it out of the box, and other IDEs need its plugin.

## Build and test

```sh
./gradlew build                     # compile, Javadoc, every test
./gradlew :dq-engine:scaleTest      # just the 1,000,000-record test in a 64 MB heap
```

`build` runs the task's fixture as an acceptance test, one or two tests for each requirement, the scale test, the JSON adapters, the SpEL compiler and the HTTP API end to end. The tests prove the requirements, not every corner case.

## Use the library

```java
Rule vatFormat = Rule.builder()
        .id("vatFormat")
        .label("VAT id is present and at least 9 characters long")
        .status(RuleStatus.RELEASED)
        .severity(Severity.ERROR)
        .categories(Set.of("tax"))
        .logic(f -> f.text("vatId").filter(v -> v.length() >= 9).map(v -> "ok").orElse("bad"))
        .mapping(new DecisionMapping(Map.of("ok", VALID, "bad", INVALID), REVIEW))
        .build();

ValidationEngine engine = ValidationEngine.create(RuleCatalog.of(vatFormat));   // immutable, thread-safe
ValidationEngine taxRules = engine.select(RuleFilter.category("tax"));          // selected once, not per record

RecordOutcome outcome = taxRules.validate(record);                              // one record's results and errors
```

A Kafka listener makes that call once per record. A host that reads a batch hands the engine a `RecordSource` and commits at each batch end:

```java
try (InputStream in = Files.newInputStream(path); JsonRecordReader records = JsonRecordReader.ofArray(in)) {
    RunSummary summary = taxRules.validateAll(records, 10_000, new RunListener() {
        public void outcome(long position, RecordOutcome outcome) { sink.write(outcome); }
        public void recordError(RecordError error) { sink.write(error); }
        public void batchEnd(RunSummary soFar) { sink.commit(); }
    });
}
```

An unreadable element becomes a `RecordError` at its position, and the run reads on. Broken JSON (`JsonInputException`) stops it.

Rules can also be data. `JsonRuleCatalog.read(json, compiler)` loads a JSON array of rule definitions, and the host's `RuleLogicCompiler` compiles each rule's `logic` string, for example with an expression language like the example's SpEL compiler. Unknown properties are ignored, so the rules service can add one before every engine knows it.

```json
[{"id": "vatFormat", "label": "VAT id is present and at least 9 characters long",
  "status": "RELEASED", "severity": "ERROR", "categories": ["tax"],
  "logic": "vatId != null && vatId.length() >= 9 ? 'ok' : 'bad'",
  "mapping": {"ok": "VALID", "bad": "INVALID", "default": "REVIEW"}}]
```

## Run the example HTTP API

```sh
./gradlew :dq-example-http:run          # listens on $PORT, default 8080
```

It loads its rules at startup. With no settings it uses the bundled [`rules.json`](dq-example-http/src/main/resources/rules.json) (the task's three fixture rules, in SpEL), so it runs without any other service. In a deployment, `dq.rules-url` points at a rules service that answers `GET` with the same JSON array:

```sh
./gradlew :dq-example-http:run --args='--dq.rules-url=http://rules-service/v1/rules'
```

If the rules can't be loaded, the app doesn't start: an app without valid rules has nothing to serve. The fetch has no timeouts or retries; a production client would add them.

Validate the task's fixture:

```sh
curl -s -X POST localhost:8080/v1/validations \
     -H 'Content-Type: application/json' \
     --data-binary @dq-engine/src/testFixtures/resources/fixtures/records.json
```

The response holds one outcome per record, in input order, each with its results (and their provenance) and errors, and then the summary (3 records, 9 results, 0 errors). To see fault isolation, send a record whose `iban` is a number. That rule reports an error inside the record's outcome, and every other rule still produces its result in the same `200` response:

```sh
curl -s -X POST 'localhost:8080/v1/validations?category=banking&category=tax' \
     -H 'Content-Type: application/json' \
     -d '[{"id":"r4","vatId":"DE444444444","country":"DE","iban":12345}]'
```

```json
{"outcomes":[{"position":0,"recordId":"r4",
              "results":[{"decision":"VALID","severity":"ERROR","provenance":{"ruleId":"vatFormat", ...}}],
              "errors":[{"type":"RULE","ruleId":"ibanFormat","message":"EL1004E: Method call: ...", ...}]}],
 "summary":{"records":1,"results":1,"errors":1, ...}}
```

Query parameters: `category` (repeatable) and `countryScope=true|false`. A body that isn't a JSON array, malformed JSON or a bad query parameter gets Spring's `400 application/problem+json`. An element without an `id`, or a `null` element, is reported as a `RECORD` error at its position, and the other records still validate. A request takes at most 1,000 records, validated as one batch; a larger one gets `413`. Larger inputs belong in a worker that streams them through `validateAll`.
