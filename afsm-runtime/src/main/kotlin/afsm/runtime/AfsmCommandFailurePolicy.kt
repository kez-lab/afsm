package afsm.runtime

/**
 * Controls how AfsmHost reacts when a command handler throws.
 */
public enum class AfsmCommandFailurePolicy {
    /**
     * Record a diagnostic and keep the host alive for later events.
     *
     * Diagnostics are sent to [AfsmConfig.logger]. If the logger is
     * [AfsmLogger.None], the failure is effectively ignored after it is
     * converted into a diagnostic.
     */
    Record,

    /**
     * Rethrow the failure from the processing coroutine.
     */
    Throw,
}
