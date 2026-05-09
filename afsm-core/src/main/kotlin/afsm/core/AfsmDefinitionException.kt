package afsm.core

/**
 * Thrown when an executable Afsm DSL definition is internally inconsistent.
 */
public class AfsmDefinitionException(
    message: String,
) : IllegalArgumentException(message)
