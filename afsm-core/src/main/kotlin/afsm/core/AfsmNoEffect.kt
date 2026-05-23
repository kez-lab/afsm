package afsm.core

/**
 * Marker effect type for machines that do not emit UI one-shot effects.
 *
 * No implementation should be created. Use this as the `Effect` type argument
 * when a machine only produces state and commands.
 */
public sealed interface AfsmNoEffect
