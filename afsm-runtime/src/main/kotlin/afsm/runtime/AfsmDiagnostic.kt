package afsm.runtime

import afsm.core.AfsmDecision

public class AfsmDiagnostic(
    public val state: Any,
    public val event: Any,
    public val decision: AfsmDecision,
    public val reason: String?,
    public val message: String,
)
