package com.projectfilenotes.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.*

class NoteDialog(project: Project, private val filePath: String, initialContent: String = "") : DialogWrapper(project) {
    private val textArea = JTextArea(15, 60)

    init {
        title = "Edit Note for $filePath"
        textArea.text = initialContent
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(JScrollPane(textArea))
        val hint = JLabel("支持 Markdown 文本。每个文件对应一个独立的批注文件。")
        panel.add(hint)
        return panel
    }

    fun getMarkdown(): String = textArea.text
}
