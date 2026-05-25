package afsm.core

/**
 * Marker command type for machines that do not emit host-executed work.
 *
 * No implementation should be created. Use this as the `Command` type argument
 * when a machine only produces state and optional effects.
 */
public sealed interface AfsmNoCommand
