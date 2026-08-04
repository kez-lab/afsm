package afsm.runtime

/**
 * Stable category for an Afsm runtime diagnostic.
 *
 * Codes contain no domain values and are safe to use for metrics or grouping.
 */
public enum class AfsmDiagnosticCode {
    InvalidTransition,

    /** The reducer threw instead of returning a transition. */
    ReducerFailure,
    IgnoredTransitionOutputDropped,
    CommandFailure,

    /** A phase-owned invocation key was started while still active. */
    DuplicateInvocationKey,

    /** A dispatched event was rejected by a full or closed event queue. */
    EventDropped,
    CommandQueueOverflow,
    CommandResultQueueOverflow,
    CommandResultDroppedHostClosed,

    /** A processing coroutine stopped because of a failure. */
    HostStopped,
}
