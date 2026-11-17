# Design: dq-engine

## Public API

```java
ValidationEngine engine = ValidationEngine.create(catalog);
ValidationEngine taxRules = engine.select(RuleFilter.category("tax").and(RuleFilter.countryScope()));

RecordOutcome outcome = taxRules.validate(record);                            // a listener: one record
RunSummary summary = taxRules.validateAll(jsonRecords, 10_000, runListener);  // a file, a cursor, a request
```

- **One call per record, and a batch built on it.** Kafka/RabbitMQ listeners and Spring Batch already own a loop with commits and retries, so they call `validate` per record. Hosts that pull records call `validateAll`, which owns the loop: it reports an unreadable record as a `RecordError` and reads on, hands each outcome to the listener, totals the `RunSummary` and calls `batchEnd` every `batchSize` records, the host's commit point.
- **The engine is immutable.** `create` reads the catalog once, so one engine can be shared across threads. `select` returns a narrower engine. To reload rules, create a new one.
- **Selection is resolved once.** A `RuleFilter` has a part over the rule (status, category), which `select` applies once, and a part over the record (country scope), which runs per record on the selected rules only. With 1,500 rules and a filter that keeps 30, a record costs 30 checks.
- **Logic computes a value, and the mapping decides (F2).** `RuleLogic` returns a `String`. `DecisionMapping` is a table with a mandatory default.
- **Provenance is recorded, not declared.** Logic reads fields through `RecordFields`, which records every read, including reads of absent fields.

## Fault isolation and bounded memory

Only the rule's logic runs inside `try`/`catch (Exception)`. A failure becomes a `RuleError` in that record's outcome, with the rule id, record id, message and the fields read so far. The other rules still run. A record that can't be read (`InvalidRecordException`) becomes a `RecordError` at its position, and the run reads on. `Error`s, broken JSON and exceptions from a filter or the listener propagate, because they aren't the rule's fault.

`validateAll` holds one record and its outcome at a time and keeps only the summary. `JsonRecordReader` parses one array element at a time. `ScaleTest` streams 1,000,000 JSON records through both in a 64 MB heap.

## Seams

| Seam                | Shipped                                    | Host supplies                         |
|---------------------|--------------------------------------------|---------------------------------------|
| `RuleCatalog`       | `RuleCatalog.of(...)`, `JsonRuleCatalog`   | Rules from a DB or a service          |
| `RuleLogicCompiler` | The interface only                         | An expression language (the example uses SpEL) |
| `RuleFilter`        | `status`, `category`, `countryScope`, `and` | `RuleFilter.of(rule -> ...)`         |
| `RecordSource`      | `JsonRecordReader`, `RecordSource.of(maps)` | A cursor, a queue, any source       |
| `RunListener`       | The interface only                         | Where outcomes go, and the commit     |

## Assumptions

1. Records are flat JSON objects with a non-blank string `id`. `null` means absent; `""` counts as present.
2. `vatFormat` and `ibanFormat` have no default in the task, so I used `REVIEW`. I also chose the severities and categories of all three rules.
3. Country scope is an opt-in filter. A rule the filter rejects produces no result at all, not `NOT_APPLICABLE`.
4. The severity breakdown counts every result, including `VALID` ones.
5. Rule ids are unique among `RELEASED` rules. A draft of the next version may share the released rule's id.

## Trade-offs and what I'd do next

- `try`/`catch` can't stop a rule that hangs. **Next:** per-rule timeouts on virtual threads.
- Country scope is checked per selected rule. **Next:** index the rules by country, so a record touches only `WORLD` rules and its own country's.
- The catalog reader ignores unknown properties, so the rules service can add one before every engine knows it; a typo in an optional property goes unnoticed. **Next:** a schema check where rules are authored.
- Corner cases are kept simple: one error for any invalid catalog or malformed JSON, one or two tests per requirement. **Next:** precise errors and edge-case tests.
- The HTTP example holds a request and its outcomes in memory, so it takes at most 1,000 records, as one batch. Spring reads the body before the count is checked, so a limit in bytes belongs to a gateway. Bulk runs belong in a worker. **Next:** Kafka and Spring Batch example hosts.
- **Next:** lookups to reference data (sanctions lists, registers) behind a port with caching, rule versions in provenance, and nested field paths.
