package afsm.runtime

/**
 * Receives runtime diagnostics.
 *
 * Diagnostics are types-only by default. If the host explicitly uses
 * [AfsmDiagnosticDataPolicy.IncludeValues], the logger becomes responsible for
 * preventing raw domain values from reaching production logs or crash tools.
 */
public fun interface AfsmLogger {
    public fun log(diagnostic: AfsmDiagnostic)

    public companion object {
        public val None: AfsmLogger = AfsmLogger { }
    }
}
