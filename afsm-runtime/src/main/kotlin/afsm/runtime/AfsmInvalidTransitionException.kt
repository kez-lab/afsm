package afsm.runtime

public class AfsmInvalidTransitionException(
    public val diagnostic: AfsmDiagnostic,
) : IllegalStateException(diagnostic.message)
