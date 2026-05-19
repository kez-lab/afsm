package afsm.runtime

/**
 * Thrown when the host event queue rejects an event produced by command work.
 *
 * Command result events use the same bounded event queue as external dispatch.
 * If that queue is full, the runtime fails fast instead of suspending the
 * sequential command processor indefinitely.
 */
public class AfsmEventQueueOverflowException(
    public val diagnostic: AfsmDiagnostic,
) : IllegalStateException(diagnostic.message)
