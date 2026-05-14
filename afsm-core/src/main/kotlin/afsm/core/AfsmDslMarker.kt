package afsm.core

/**
 * Marks Afsm DSL receiver scopes so nested `state`, `on`, and transition
 * blocks do not accidentally call functions from an outer DSL scope.
 */
@DslMarker
public annotation class AfsmDslMarker
