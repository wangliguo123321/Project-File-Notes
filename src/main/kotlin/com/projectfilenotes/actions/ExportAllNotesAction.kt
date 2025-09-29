package com.projectfilenotes.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.projectfilenotes.NotesService
import java.io.File

class ExportAllNotesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = project.getService(NotesService::class.java)
        
        // 选择导出目录
        val dirDescriptor = FileChooserDescriptor(false, true, false, false, false, false)
        dirDescriptor.title = "选择导出目录"
        val targetDir = FileChooser.chooseFile(dirDescriptor, project, null) ?: return
        
        if (!targetDir.isDirectory) {
            Messages.showMessageDialog(project, "请选择一个目录", "导出失败", Messages.getErrorIcon())
            return
        }
        
        try {
            val notes = service.getAllNoteFiles()
            if (notes.isEmpty()) {
                Messages.showInfoMessage(project, "暂无批注文件可导出", "导出完成")
                return
            }
            
            var exportedCount = 0
            notes.forEach { (filePath, content) ->
                val fileName = filePath.replace("/", "_").replace("\\", "_") + ".md"
                val targetFile = File(targetDir.path, fileName)
                targetFile.writeText(content, Charsets.UTF_8)
                exportedCount++
            }
            
            // 同时导出汇总文件
            val summaryContent = service.exportAllNotesAsMarkdown()
            val summaryFile = File(targetDir.path, "所有批注汇总.md")
            summaryFile.writeText(summaryContent, Charsets.UTF_8)
            
            Messages.showInfoMessage(project, "成功导出 $exportedCount 个批注文件到：${targetDir.path}", "导出成功")
            
        } catch (ex: Exception) {
            Messages.showMessageDialog(project, "导出失败：${ex.message}", "导出错误", Messages.getErrorIcon())
        }
    }
}
