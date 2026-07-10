package afsm.runtime

/**
 * Value-free decision category captured by a runtime diagnostic.
 */
public enum class AfsmDiagnosticDecision {
    Transitioned,
    Handled,
    Ignored,
    Invalid,
}
