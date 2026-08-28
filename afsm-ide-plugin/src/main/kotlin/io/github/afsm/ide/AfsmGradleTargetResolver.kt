package io.github.afsm.ide

import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import java.nio.file.InvalidPathException
import java.nio.file.Path
import org.jetbrains.plugins.gradle.util.GradleConstants

internal data class AfsmGraphTarget(
    val rootProjectPath: Path,
    val moduleDirectory: Path,
    val taskPath: String,
    val graph: AfsmGraphSource,
    val paths: AfsmGraphPaths,
)

internal sealed interface AfsmTargetResolution {
    data class Resolved(val target: AfsmGraphTarget) : AfsmTargetResolution
    data class Error(val message: String) : AfsmTargetResolution
}

internal object AfsmGradleTargetResolver {
    fun resolve(element: PsiElement, graph: AfsmGraphSource): AfsmTargetResolution {
        if (!AfsmGraphFileResolver.isSafeMmdFileName(graph.fileName)) {
            return AfsmTargetResolution.Error(
                "Afsm graph fileName must be a safe relative .mmd path: ${graph.fileName}",
            )
        }

        val module = ModuleUtilCore.findModuleForPsiElement(element)
            ?: return AfsmTargetResolution.Error("The Afsm graph is not inside an IDE module.")
        val properties = ExternalSystemModulePropertyManager.getInstance(module)
        if (properties.getExternalSystemId() != GradleConstants.SYSTEM_ID.id) {
            return AfsmTargetResolution.Error(
                "The module '${module.name}' is not linked to Gradle.",
            )
        }

        val rootProjectPath = properties.getRootProjectPath()
            ?: return AfsmTargetResolution.Error("The linked Gradle root path is unavailable.")
        val moduleDirectory = properties.getLinkedProjectPath()
            ?: return AfsmTargetResolution.Error("The linked Gradle module path is unavailable.")
        val externalProjectId = properties.getLinkedProjectId()
            ?: return AfsmTargetResolution.Error("The linked Gradle project id is unavailable.")
        val isSourceSet = properties.getExternalModuleType() ==
            GradleConstants.GRADLE_SOURCE_SET_MODULE_TYPE_KEY
        val identityPath = GradleIdentityPath.normalize(externalProjectId, isSourceSet)

        return try {
            val modulePath = Path.of(moduleDirectory)
            AfsmTargetResolution.Resolved(
                AfsmGraphTarget(
                    rootProjectPath = Path.of(rootProjectPath),
                    moduleDirectory = modulePath,
                    taskPath = GradleIdentityPath.generationTask(identityPath),
                    graph = graph,
                    paths = AfsmGraphFileResolver.resolve(modulePath, graph.fileName),
                ),
            )
        } catch (error: InvalidPathException) {
            AfsmTargetResolution.Error("The linked Gradle path is invalid: ${error.input}")
        }
    }
}
