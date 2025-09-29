package com.projectfilenotes.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.projectfilenotes.NotesService

class ExportNotesMarkdownAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = project.getService(NotesService::class.java)
        val text = service.exportAllNotesAsMarkdown()
        val saveDescriptor = FileChooserDescriptor(false, false, false, false, false, true)
        val target = FileChooser.chooseFile(saveDescriptor, project, null) ?: return
        VfsUtil.saveText(target, text)
        Messages.showInfoMessage(project, "已导出 Markdown 到：${target.path}", "导出成功")
    }
}

class ImportNotesMarkdownAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val openDescriptor = FileChooserDescriptor(true, false, false, false, false, false)
        val file = FileChooser.chooseFile(openDescriptor, project, null) ?: return
        val text = VfsUtil.loadText(file)
        project.getService(NotesService::class.java).importFromMarkdown(text)
        Messages.showInfoMessage(project, "已从 Markdown 导入：${file.path}", "导入成功")
    }
}