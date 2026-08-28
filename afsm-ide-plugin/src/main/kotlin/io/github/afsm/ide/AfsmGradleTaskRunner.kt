package io.github.afsm.ide

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.externalSystem.util.task.TaskExecutionSpec
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.gradle.util.GradleConstants

internal fun interface AfsmGradleTaskRunner {
    fun generate(project: Project, target: AfsmGraphTarget, onComplete: (Boolean) -> Unit)
}

internal class ExternalSystemAfsmGradleTaskRunner : AfsmGradleTaskRunner {
    override fun generate(
        project: Project,
        target: AfsmGraphTarget,
        onComplete: (Boolean) -> Unit,
    ) {
        // Android Studio 261 asserts that document saving happens under a
        // write action; a toolbar click runs on the EDT but does not hold it.
        ApplicationManager.getApplication().runWriteAction {
            FileDocumentManager.getInstance().saveAllDocuments()
        }

        val settings = ExternalSystemTaskExecutionSettings().apply {
            executionName = "Generate Afsm graph '${target.graph.id}'"
            externalSystemIdString = GradleConstants.SYSTEM_ID.id
            externalProjectPath = target.rootProjectPath.toString()
            taskNames = listOf(target.taskPath)
        }
        val callback = object : TaskCallback {
            override fun onSuccess() = finish(true)

            override fun onFailure() = finish(false)

            private fun finish(success: Boolean) {
                ApplicationManager.getApplication().invokeLater {
                    if (!project.isDisposed) onComplete(success)
                }
            }
        }

        val spec = TaskExecutionSpec.create()
            .withProject(project)
            .withSystemId(GradleConstants.SYSTEM_ID)
            .withExecutorId(DefaultRunExecutor.EXECUTOR_ID)
            .withSettings(settings)
            .withProgressExecutionMode(ProgressExecutionMode.IN_BACKGROUND_ASYNC)
            .withCallback(callback)
            .withActivateToolWindowBeforeRun(false)
            .withActivateToolWindowOnFailure(true)
            .dontNavigateToError()
            .build()

        runCatching { ExternalSystemUtil.runTask(spec) }
            .onFailure { callback.onFailure() }
    }
}
