/** Data-quality rule engine. */
module io.dqengine.core {
    requires static lombok;
    requires tools.jackson.core;
    requires tools.jackson.databind;

    exports io.dqengine.core.domain;
    exports io.dqengine.core.port.in;
    exports io.dqengine.core.port.out;
    exports io.dqengine.core.adapter.json;

    opens io.dqengine.core.adapter.json to tools.jackson.databind;
}
