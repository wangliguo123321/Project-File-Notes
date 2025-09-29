package com.projectfilenotes.decorators

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.projectfilenotes.NotesService
import java.nio.file.Paths
import javax.swing.Icon

class NotesFileIconProvider : FileIconProvider {
    private val badge: Icon = com.intellij.openapi.util.IconLoader.getIcon("/icons/noteBadge.svg", javaClass)

    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? {
        val pj = project ?: ProjectManager.getInstance().openProjects.firstOrNull() ?: return null
        val base = pj.basePath ?: return null
        val relative = try { Paths.get(base).relativize(Paths.get(file.path)).toString() } catch (_: Exception) { return null }
        val hasNote = pj.getService(NotesService::class.java).getNoteForFile(relative).isNotBlank()
        return if (hasNote) badge else null
    }
}
