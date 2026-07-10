package afsm.runtime

/**
 * Privacy-aware description of a runtime problem.
 *
 * The top-level envelope contains only library-owned text and simple Kotlin
 * type names. Raw domain values are available through [values] only after the
 * caller explicitly selects [AfsmDiagnosticDataPolicy.IncludeValues].
 */
public class AfsmDiagnostic internal constructor(
    public val code: AfsmDiagnosticCode,
    public val decision: AfsmDiagnosticDecision,
    public val message: String,
    public val stateType: String,
    public val eventType: String,
    public val commandType: String?,
    public val failureType: String?,
    public val metadata: Map<String, String>,
    public val values: AfsmDiagnosticValues?,
)
