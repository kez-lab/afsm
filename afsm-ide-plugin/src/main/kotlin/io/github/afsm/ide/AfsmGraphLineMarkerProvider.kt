package io.github.afsm.ide

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import javax.swing.Icon

class AfsmGraphLineMarkerProvider : LineMarkerProviderDescriptor(), DumbAware {
    override fun getName(): String = "Afsm graph preview"

    override fun getIcon(): Icon = AfsmIcons.Graph

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // Kotlin exposes the annotation name through several nested PSI nodes.
        // A gutter marker belongs to the identifier leaf only, otherwise each
        // ancestor creates a duplicate icon on the same line.
        if (element.firstChild != null) return null
        if (element.text != "AfsmGraph") return null
        val file = element.containingFile ?: return null
        val graph = AfsmGraphSourceParser.parseAt(file.text, element.textRange.startOffset)
            ?: return null

        return LineMarkerInfo(
            element,
            element.textRange,
            AfsmIcons.Graph,
            { "Preview Afsm graph '${graph.id}'" },
            GutterIconNavigationHandler { _, clickedElement ->
                openPreview(clickedElement, graph)
            },
            GutterIconRenderer.Alignment.CENTER,
            { "Afsm Graph Preview" },
        )
    }

    private fun openPreview(element: PsiElement, graph: AfsmGraphSource) {
        val service = element.project.getService(AfsmGraphPreviewService::class.java)
        when (val resolution = AfsmGradleTargetResolver.resolve(element, graph)) {
            is AfsmTargetResolution.Resolved -> service.open(resolution.target)
            is AfsmTargetResolution.Error -> service.openError(resolution.message)
        }
    }
}
