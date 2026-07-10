package afsm.core

/**
 * Marks a state machine class, object, or stable top-level property as a graph
 * source for generated `.mmd` output.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
)
@Retention(AnnotationRetention.BINARY)
public annotation class AfsmGraph(
    public val id: String = "",
    public val fileName: String = "",
)
