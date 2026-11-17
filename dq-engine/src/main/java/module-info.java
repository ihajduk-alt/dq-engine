/** Data-quality rule engine. */
module io.dqengine.core {
    requires static lombok;

    exports io.dqengine.core.domain;
    exports io.dqengine.core.port.in;
    exports io.dqengine.core.port.out;
}
