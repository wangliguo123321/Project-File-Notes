package com.projectfilenotes.listeners

import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.editor.Editor
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.projectfilenotes.NotesService
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.JPanel

class EditorOpenListener : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val project = editor.project ?: ProjectManager.getInstance().openProjects.firstOrNull() ?: return
        val vFile = editor.document?.let { doc ->
            com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(doc)
        } ?: return
        val base = project.basePath ?: return
        val path = vFile.path
        if (!path.startsWith(base)) return
        val relative = path.removePrefix(base).trimStart('/')

        val service = project.getService(NotesService::class.java)
        val noteContent = service.getNoteForFile(relative)
        if (noteContent.isBlank()) return

        val panel = JPanel(BorderLayout())
        panel.background = JBColor(Color(255, 255, 204), Color(60, 63, 65))
        panel.border = BorderFactory.createEmptyBorder(4, 8, 4, 8)
        val label = JBLabel("该文件存在批注，右键可编辑。内容预览：${noteContent.take(100)}${if (noteContent.length > 100) "..." else ""}")
        panel.add(label, BorderLayout.CENTER)

        EditorBanner.attach(editor, panel)
    }
}

object EditorBanner {
    fun attach(editor: Editor, component: JPanel) {
        val contentPanel = editor.component.parent
        if (contentPanel is JPanel) {
            contentPanel.add(component, BorderLayout.NORTH)
            contentPanel.revalidate()
            contentPanel.repaint()
        }
    }
}