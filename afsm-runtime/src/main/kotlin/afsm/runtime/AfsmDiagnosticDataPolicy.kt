package afsm.runtime

/**
 * Controls whether Afsm diagnostics retain raw domain values.
 */
public enum class AfsmDiagnosticDataPolicy {
    /**
     * Expose diagnostic codes, fixed messages, and simple Kotlin type names.
     * Raw state, event, command, reason, and throwable values are discarded.
     */
    TypesOnly,

    /**
     * Retain raw values in [AfsmDiagnostic.values].
     *
     * This may expose credentials, personal data, tokens, form input, or
     * exception details. Do not use it for production logging without an
     * application-owned redaction boundary.
     */
    IncludeValues,
}
