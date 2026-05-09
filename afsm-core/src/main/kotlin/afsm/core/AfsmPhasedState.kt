package afsm.core

public interface AfsmPhasedState<S : Any, P : Any, X : Any> {
    public val phase: P

    public val context: X

    public fun with(
        phase: P,
        context: X,
    ): S
}
