package io.github.afsm.ide

import java.nio.file.Path

internal enum class AfsmGraphOrigin {
    CHECKED_IN,
    GENERATED,
}

internal data class AfsmGraphDocument(
    val source: String,
    val path: Path,
    val origin: AfsmGraphOrigin,
)

internal sealed interface AfsmPreviewState {
    data object Empty : AfsmPreviewState
    data class Loading(val previous: AfsmGraphDocument?) : AfsmPreviewState
    data class Ready(val document: AfsmGraphDocument) : AfsmPreviewState
    data class Stale(
        val document: AfsmGraphDocument,
        val message: String,
    ) : AfsmPreviewState
    data class Error(val message: String) : AfsmPreviewState

    companion object {
        fun startRefresh(current: AfsmPreviewState): Loading = Loading(current.documentOrNull())

        fun failRefresh(current: AfsmPreviewState, message: String): AfsmPreviewState {
            val previous = current.documentOrNull()
            return if (previous == null) Error(message) else Stale(previous, message)
        }

        private fun AfsmPreviewState.documentOrNull(): AfsmGraphDocument? = when (this) {
            Empty, is Error -> null
            is Loading -> previous
            is Ready -> document
            is Stale -> document
        }
    }
}
