package io.github.afsm.ide

import java.nio.file.Path

internal data class AfsmGraphPaths(
    val checkedIn: Path,
    val generated: Path,
) {
    val initialCandidates: List<Path> = listOf(checkedIn, generated)
    val afterRefresh: Path = generated
}

internal object AfsmGraphFileResolver {
    fun resolve(moduleDirectory: Path, fileName: String): AfsmGraphPaths {
        require(isSafeMmdFileName(fileName)) {
            "Afsm graph fileName must be a safe relative .mmd path: $fileName"
        }
        return AfsmGraphPaths(
            checkedIn = moduleDirectory.resolve("afsm-graph").resolve(fileName),
            generated = moduleDirectory.resolve("build/generated/afsm/mmd").resolve(fileName),
        )
    }

    fun isSafeMmdFileName(fileName: String): Boolean {
        if (fileName.isBlank() || !fileName.endsWith(".mmd")) return false
        if (fileName.startsWith('/') || fileName.startsWith('\\')) return false
        return fileName.split('/', '\\').all { segment ->
            segment.isNotBlank() && segment != "." && segment != ".."
        }
    }
}
