package afsm.runtime

/**
 * Explicitly retained raw values for a diagnostic.
 *
 * This object exists only when [AfsmDiagnosticDataPolicy.IncludeValues] is
 * configured. Accessing or logging these values requires an application-owned
 * privacy and redaction decision.
 */
public class AfsmDiagnosticValues internal constructor(
    public val state: Any,
    public val event: Any,
    public val command: Any?,
    public val reason: String?,
    public val throwable: Throwable?,
)
