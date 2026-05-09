package afsm.runtime

/**
 * Controls how AfsmHost reacts when a command handler throws.
 */
public enum class AfsmCommandFailurePolicy {
    /**
     * Record a diagnostic and keep the host alive for later events.
     */
    Record,

    /**
     * Rethrow the failure from the processing coroutine.
     */
    Throw,
}
