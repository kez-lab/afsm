package afsm.runtime

/**
 * Controls how commands emitted by accepted transitions are executed.
 */
public enum class AfsmCommandExecutionPolicy {
    /**
     * Executes commands one at a time in emission order.
     *
     * Command execution is separated from event reduction, so a suspended
     * command does not prevent later UI events from being reduced.
     */
    Sequential,
}
