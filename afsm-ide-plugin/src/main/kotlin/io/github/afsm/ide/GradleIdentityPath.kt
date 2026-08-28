package io.github.afsm.ide

internal object GradleIdentityPath {
    fun normalize(externalProjectId: String, isSourceSet: Boolean): String {
        val usesAbsoluteIdentity = externalProjectId.startsWith(':')
        val segments = externalProjectId
            .trim(':')
            .split(':')
            .filter(String::isNotBlank)
            .toMutableList()

        if (!usesAbsoluteIdentity && segments.isNotEmpty()) {
            segments.removeAt(0)
        }
        if (isSourceSet && segments.isNotEmpty()) {
            segments.removeAt(segments.lastIndex)
        }

        return if (segments.isEmpty()) ":" else ":${segments.joinToString(":")}"
    }

    fun generationTask(identityPath: String): String =
        if (identityPath == ":") ":generateAfsmMmd" else "$identityPath:generateAfsmMmd"
}
