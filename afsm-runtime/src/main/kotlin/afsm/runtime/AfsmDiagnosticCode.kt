package afsm.runtime

/**
 * Stable category for an Afsm runtime diagnostic.
 *
 * Codes contain no domain values and are safe to use for metrics or grouping.
 */
public enum class AfsmDiagnosticCode {
    InvalidTransition,
    IgnoredTransitionOutputDropped,
    CommandFailure,
    CommandQueueOverflow,
    CommandResultQueueOverflow,
    CommandResultDroppedHostClosed,
}
