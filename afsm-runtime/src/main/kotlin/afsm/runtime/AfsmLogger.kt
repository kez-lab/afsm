package afsm.runtime

public fun interface AfsmLogger {
    public fun log(diagnostic: AfsmDiagnostic)

    public companion object {
        public val None: AfsmLogger = AfsmLogger { }
    }
}
