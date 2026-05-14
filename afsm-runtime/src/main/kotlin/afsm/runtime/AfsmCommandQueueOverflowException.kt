package afsm.runtime

/**
 * Thrown when an accepted transition emits a command but the host command
 * queue cannot accept it.
 *
 * This is treated as a runtime pressure error instead of suspending the event
 * processor indefinitely. Increase [AfsmConfig.commandQueueCapacity] or model a
 * smaller number of coarser commands when this exception appears.
 */
public class AfsmCommandQueueOverflowException(
    public val diagnostic: AfsmDiagnostic,
) : IllegalStateException(diagnostic.message)
