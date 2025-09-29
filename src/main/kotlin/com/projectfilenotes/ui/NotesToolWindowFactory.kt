package com.projectfilenotes.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.content.ContentFactory
import com.projectfilenotes.NotesService
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.JPopupMenu
import javax.swing.JMenuItem
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

data class NoteFileItem(val filePath: String, val content: String)

class NotesToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val service = project.getService(NotesService::class.java)
        val listModel = javax.swing.DefaultListModel<NoteFileItem>()
        val list = JBList(listModel)
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        val search = JTextField()
        val panel = JPanel(BorderLayout())
        panel.add(search, BorderLayout.NORTH)
        panel.add(JScrollPane(list), BorderLayout.CENTER)

        fun refresh(filter: String = "") {
            listModel.clear()
            val items = service.getAllNoteFiles().filter { (filePath, content) ->
                if (filter.isBlank()) true else (
                    filePath.contains(filter, true) ||
                    content.contains(filter, true)
                )
            }.map { (filePath, content) -> NoteFileItem(filePath, content) }
            items.forEach { listModel.addElement(it) }
            list.cellRenderer = NoteFileListCellRenderer()
        }

        // 添加双击编辑和右键删除功能
        list.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    val selectedItem = list.selectedValue as? NoteFileItem ?: return
                    val dialog = NoteDialog(project, selectedItem.filePath, selectedItem.content)
                    if (dialog.showAndGet()) {
                        val newContent = dialog.getMarkdown()
                        service.saveNoteForFile(selectedItem.filePath, newContent)
                        refresh(search.text)
                    }
                }
            }
        })
        
        // 添加右键菜单
        val popupMenu = JPopupMenu()
        val editItem = JMenuItem("编辑")
        val deleteItem = JMenuItem("删除")
        
        editItem.addActionListener {
            val selectedItem = list.selectedValue as? NoteFileItem ?: return@addActionListener
            val dialog = NoteDialog(project, selectedItem.filePath, selectedItem.content)
            if (dialog.showAndGet()) {
                val newContent = dialog.getMarkdown()
                service.saveNoteForFile(selectedItem.filePath, newContent)
                refresh(search.text)
            }
        }
        
        deleteItem.addActionListener {
            val selectedItem = list.selectedValue as? NoteFileItem ?: return@addActionListener
            val result = com.intellij.openapi.ui.Messages.showYesNoDialog(
                project,
                "确定要删除文件 '${selectedItem.filePath}' 的批注吗？",
                "确认删除",
                "删除",
                "取消",
                com.intellij.openapi.ui.Messages.getQuestionIcon()
            )
            if (result == 0) { // Yes
                service.deleteNoteForFile(selectedItem.filePath)
                refresh(search.text)
            }
        }
        
        popupMenu.add(editItem)
        popupMenu.add(deleteItem)
        
        list.componentPopupMenu = popupMenu

        search.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) { refresh(search.text) }
            override fun removeUpdate(e: DocumentEvent?) { refresh(search.text) }
            override fun changedUpdate(e: DocumentEvent?) { refresh(search.text) }
        })

        refresh()

        val content = ContentFactory.getInstance().createContent(panel, "All Notes", false)
        toolWindow.contentManager.addContent(content)
    }
}
