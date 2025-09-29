package com.projectfilenotes.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.projectfilenotes.NotesService
import com.projectfilenotes.ui.NoteDialog
import java.nio.file.Paths

class AddOrEditNoteAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val base = project.basePath ?: return
        val relative = Paths.get(base).relativize(Paths.get(vFile.path)).toString()

        val service = project.getService(NotesService::class.java)
        val existingContent = service.getNoteForFile(relative)

        val dialog = NoteDialog(project, relative, existingContent)
        if (dialog.showAndGet()) {
            val content = dialog.getMarkdown()
            service.saveNoteForFile(relative, content)
        }
    }
}