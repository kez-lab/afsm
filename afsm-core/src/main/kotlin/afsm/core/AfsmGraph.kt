package afsm.core

/**
 * Marks a state machine class as a graph source for generated `.mmd` output.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class AfsmGraph(
    public val id: String = "",
    public val fileName: String = "",
)

