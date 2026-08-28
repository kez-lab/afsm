package io.github.afsm.ide

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

@Service(Service.Level.PROJECT)
class AfsmGraphPreviewService(private val project: Project) {
    private var panel: AfsmGraphPreviewPanel? = null
    private var pendingRequest: PreviewRequest? = null

    internal fun attach(panel: AfsmGraphPreviewPanel) {
        this.panel = panel
        consumePendingRequest()
    }

    internal fun open(target: AfsmGraphTarget) {
        activate(PreviewRequest.Target(target))
    }

    internal fun openError(message: String) {
        activate(PreviewRequest.Error(message))
    }

    private fun activate(request: PreviewRequest) {
        pendingRequest = request
        val show: () -> Unit = {
            ToolWindowManager.getInstance(project)
                .getToolWindow(TOOL_WINDOW_ID)
                ?.show { consumePendingRequest() }
            Unit
        }
        if (ApplicationManager.getApplication().isDispatchThread) {
            show()
        } else {
            ApplicationManager.getApplication().invokeLater(show)
        }
    }

    private fun consumePendingRequest() {
        val targetPanel = panel ?: return
        when (val request = pendingRequest ?: return) {
            is PreviewRequest.Target -> targetPanel.showTarget(request.target)
            is PreviewRequest.Error -> targetPanel.showError(request.message)
        }
        pendingRequest = null
    }

    private sealed interface PreviewRequest {
        data class Target(val target: AfsmGraphTarget) : PreviewRequest
        data class Error(val message: String) : PreviewRequest
    }

    internal companion object {
        const val TOOL_WINDOW_ID: String = "Afsm Graph"
    }
}
