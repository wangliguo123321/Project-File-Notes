package com.projectfilenotes

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@Service(Service.Level.PROJECT)
class NotesService(private val project: Project) {

    private val rwLock = ReentrantReadWriteLock()

    private val notesDir: File by lazy {
        // 存储到用户主目录下的 .project-file-notes 文件夹，避免污染项目
        val userHome = System.getProperty("user.home")
        val projectName = project.name ?: "default-project"
        File(userHome, ".project-file-notes/$projectName").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private val imagesDir: File by lazy {
        File(notesDir, "images").apply { if (!exists()) mkdirs() }
    }

    fun getNoteForFile(relativeFilePath: String): String = rwLock.read {
        val noteFile = getNoteFile(relativeFilePath)
        if (noteFile.exists()) {
            noteFile.readText(StandardCharsets.UTF_8)
        } else {
            ""
        }
    }

    fun saveNoteForFile(relativeFilePath: String, content: String) = rwLock.write {
        val noteFile = getNoteFile(relativeFilePath)
        if (!noteFile.parentFile.exists()) {
            noteFile.parentFile.mkdirs()
        }
        noteFile.writeText(content, StandardCharsets.UTF_8)
        refreshVfs(noteFile)
    }

    fun deleteNoteForFile(relativeFilePath: String) = rwLock.write {
        val noteFile = getNoteFile(relativeFilePath)
        if (noteFile.exists()) {
            noteFile.delete()
            refreshVfs(noteFile)
        }
    }

    fun getAllNoteFiles(): List<Pair<String, String>> = rwLock.read {
        if (!notesDir.exists()) return@read emptyList()
        
        notesDir.walkTopDown()
            .filter { it.isFile && it.extension == "md" }
            .map { file ->
                val relativePath = file.relativeTo(notesDir).path.replace("\\", "/")
                val content = file.readText(StandardCharsets.UTF_8)
                Pair(relativePath, content)
            }
            .toList()
    }

    fun exportAllNotesAsMarkdown(): String = rwLock.read {
        val notes = getAllNoteFiles()
        val sb = StringBuilder()
        sb.appendLine("# Project File Notes")
        sb.appendLine("导出时间: ${java.time.LocalDateTime.now()}")
        sb.appendLine("项目: ${project.name ?: "Unknown"}")
        sb.appendLine()
        
        if (notes.isEmpty()) {
            sb.appendLine("暂无批注文件。")
            return@read sb.toString()
        }
        
        notes.forEach { (filePath, content) ->
            sb.appendLine("## $filePath")
            sb.appendLine()
            sb.appendLine(content)
            sb.appendLine()
            sb.appendLine("---")
            sb.appendLine()
        }
        
        sb.toString()
    }

    fun importFromMarkdown(markdown: String) = rwLock.write {
        // 简单解析 Markdown 格式的导入
        val lines = markdown.lines()
        var currentFile = ""
        var currentContent = StringBuilder()
        
        for (line in lines) {
            if (line.startsWith("## ")) {
                // 保存前一个文件
                if (currentFile.isNotEmpty() && currentContent.isNotEmpty()) {
                    saveNoteForFile(currentFile, currentContent.toString().trim())
                }
                // 开始新文件
                currentFile = line.removePrefix("## ").trim()
                currentContent = StringBuilder()
            } else if (line.startsWith("---")) {
                // 文件结束标记
                if (currentFile.isNotEmpty() && currentContent.isNotEmpty()) {
                    saveNoteForFile(currentFile, currentContent.toString().trim())
                }
                currentFile = ""
                currentContent = StringBuilder()
            } else if (currentFile.isNotEmpty()) {
                currentContent.appendLine(line)
            }
        }
        
        // 保存最后一个文件
        if (currentFile.isNotEmpty() && currentContent.isNotEmpty()) {
            saveNoteForFile(currentFile, currentContent.toString().trim())
        }
    }

    fun saveImageBytes(fileNameHint: String, bytes: ByteArray): String {
        val safeName = fileNameHint.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val file = File(imagesDir, "${System.currentTimeMillis()}_$safeName")
        file.writeBytes(bytes)
        refreshVfs(file)
        return file.absolutePath
    }

    private fun getNoteFile(relativeFilePath: String): File {
        val safePath = relativeFilePath.replace("/", "_").replace("\\", "_")
        return File(notesDir, "$safePath.md")
    }

    private fun refreshVfs(file: File) {
        val vFile: VirtualFile? = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
        vFile?.let { VfsUtil.markDirtyAndRefresh(false, false, false, it) }
    }
}
