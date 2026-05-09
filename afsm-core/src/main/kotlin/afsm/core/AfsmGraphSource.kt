package afsm.core

/**
 * Contract for state machines that expose static graph topology.
 */
public interface AfsmGraphSource {
    public val topology: AfsmTopology
}

