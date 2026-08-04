package afsm.runtime

/**
 * Controls how AfsmHost reacts when a bounded queue cannot accept more work.
 *
 * Overflow means the host is receiving events or commands faster than it can
 * process them, which is a backpressure problem rather than a flow modelling
 * error.
 */
public enum class AfsmOverflowPolicy {
    /**
     * Record a diagnostic, drop the rejected event or command, and keep the
     * host alive.
     *
     * Dropping one input keeps the screen responsive. The dropped work is
     * always reported through [AfsmConfig.logger].
     */
    Record,

    /**
     * Throw [AfsmEventQueueOverflowException] or
     * [AfsmCommandQueueOverflowException] instead of dropping work.
     *
     * This fails fast during development but stops the host, so the hosted
     * screen accepts no further events.
     */
    Throw,
}
