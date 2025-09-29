package com.projectfilenotes.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.projectfilenotes.NotesService
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import java.awt.datatransfer.DataFlavor

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import java.awt.Toolkit
import java.awt.CardLayout
import java.awt.event.KeyEvent

class FileNotesViewer : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val service = project.getService(NotesService::class.java)

        // 编辑区（隐藏在单列模式中，需切换进入）
        val textArea = JBTextArea()
        textArea.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        textArea.wrapStyleWord = true
        textArea.lineWrap = true
        textArea.isEditable = true
        val editorScroll = JBScrollPane(textArea)

        // Markdown 预览区（HTML）
        val previewPane = JEditorPane()
        previewPane.contentType = "text/html"
        previewPane.isEditable = false
        val previewScroll = JBScrollPane(previewPane)

        // 单列卡片布局：默认显示预览，需要时切换到编辑
        val card = JPanel(CardLayout())
        card.add(previewScroll, "preview")
        card.add(editorScroll, "editor")

        val panel = JPanel(BorderLayout())
        panel.add(card, BorderLayout.CENTER)

        // Markdown 渲染器
        val options = MutableDataSet()
        val parser: Parser = Parser.builder(options).build()
        val renderer: HtmlRenderer = HtmlRenderer.builder(options).build()

        fun renderPreview(md: String) {
            val document = parser.parse(md)
            val html = renderer.render(document)
            previewPane.text = "<html><head><meta charset=\"utf-8\"/></head><body>$html</body></html>"
            previewPane.caretPosition = 0
        }

        fun showPreview() {
            val cl = card.layout as CardLayout
            cl.show(card, "preview")
        }

        fun showEditor() {
            val cl = card.layout as CardLayout
            cl.show(card, "editor")
            textArea.requestFocusInWindow()
        }

        var currentFilePath: String? = null
        var isContentChanged = false

        val autoSaveTimer = Timer(1200) {
            if (isContentChanged && currentFilePath != null) {
                service.saveNoteForFile(currentFilePath!!, textArea.text)
                isContentChanged = false
                // 保存后同步预览
                renderPreview(textArea.text)
            }
        }
        autoSaveTimer.isRepeats = false

        // 文本变化：自动保存（去抖）
        textArea.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                isContentChanged = true
                autoSaveTimer.restart()
            }
            override fun removeUpdate(e: DocumentEvent?) {
                isContentChanged = true
                autoSaveTimer.restart()
            }
            override fun changedUpdate(e: DocumentEvent?) {
                isContentChanged = true
                autoSaveTimer.restart()
            }
        })

        // 预览区双击进入编辑
        previewPane.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    showEditor()
                }
            }
        })

        // 快捷键切换：Cmd/Ctrl+E 切换编辑/预览
        val toggleKey = KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx)
        panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(toggleKey, "toggle-editor")
        panel.actionMap.put("toggle-editor", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                val cl = card.layout as CardLayout
                // 简单判断：如果预览可见则进入编辑，否则回到预览
                val isShowingPreview = previewScroll.isShowing
                if (isShowingPreview) {
                    showEditor()
                } else {
                    // 切回预览前，触发一次保存与渲染
                    if (currentFilePath != null) {
                        service.saveNoteForFile(currentFilePath!!, textArea.text)
                        isContentChanged = false
                        renderPreview(textArea.text)
                    }
                    showPreview()
                }
            }
        })

        // 编辑区失焦时切回预览并保存
        textArea.addFocusListener(object : java.awt.event.FocusAdapter() {
            override fun focusLost(e: java.awt.event.FocusEvent) {
                if (currentFilePath != null) {
                    service.saveNoteForFile(currentFilePath!!, textArea.text)
                    isContentChanged = false
                    renderPreview(textArea.text)
                }
                showPreview()
            }
        })

        // 粘贴图片到编辑器：将图片保存到 images 目录并插入 Markdown 链接
        textArea.addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyPressed(e: java.awt.event.KeyEvent) {
                if (e.isControlDown && e.keyCode == java.awt.event.KeyEvent.VK_V ||
                    e.isMetaDown && e.keyCode == java.awt.event.KeyEvent.VK_V) {
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    val contents = clipboard.getContents(null)
                    if (contents != null) {
                        try {
                            when {
                                contents.isDataFlavorSupported(DataFlavor.imageFlavor) -> {
                                    val image = contents.getTransferData(DataFlavor.imageFlavor) as java.awt.Image
                                    val buffered = java.awt.image.BufferedImage(
                                        image.getWidth(null), image.getHeight(null), java.awt.image.BufferedImage.TYPE_INT_ARGB
                                    )
                                    val g = buffered.graphics
                                    g.drawImage(image, 0, 0, null)
                                    g.dispose()
                                    val baos = java.io.ByteArrayOutputStream()
                                    javax.imageio.ImageIO.write(buffered, "png", baos)
                                    val bytes = baos.toByteArray()
                                    val path = service.saveImageBytes("pasted.png", bytes)
                                    val mdLink = "![]($path)"
                                    textArea.insert(mdLink, textArea.caretPosition)
                                }
                                contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor) -> {
                                    val files = contents.getTransferData(DataFlavor.javaFileListFlavor) as java.util.List<*>
                                    if (files.isNotEmpty()) {
                                        val file = files[0] as java.io.File
                                        val bytes = file.readBytes()
                                        val path = service.saveImageBytes(file.name, bytes)
                                        val mdLink = "![]($path)"
                                        textArea.insert(mdLink, textArea.caretPosition)
                                    }
                                }
                            }
                        } catch (_: Exception) { }
                    }
                }
            }
        })

        // 监听文件变化
        val fileSelectionListener = FileSelectionListener(project, textArea, service) { filePath ->
            if (isContentChanged && currentFilePath != null) {
                service.saveNoteForFile(currentFilePath!!, textArea.text)
                isContentChanged = false
            }
            currentFilePath = filePath
            // 切换文件后总是回到预览并渲染当前内容
            renderPreview(textArea.text)
            showPreview()
        }
        project.messageBus.connect().subscribe(
            com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER,
            fileSelectionListener
        )

        val content = com.intellij.ui.content.ContentFactory.getInstance().createContent(panel, "File Notes", false)
        toolWindow.contentManager.addContent(content)
    }
}

class FileSelectionListener(
    private val project: Project,
    private val textArea: JTextArea,
    private val service: NotesService,
    private val onFileChanged: (String) -> Unit
) : com.intellij.openapi.fileEditor.FileEditorManagerListener {
    
    override fun fileOpened(source: com.intellij.openapi.fileEditor.FileEditorManager, file: VirtualFile) {
        updateNotesForFile(file)
    }
    
    override fun selectionChanged(event: com.intellij.openapi.fileEditor.FileEditorManagerEvent) {
        val newFile = event.newFile
        if (newFile != null) {
            updateNotesForFile(newFile)
        }
    }
    
    private fun updateNotesForFile(vFile: VirtualFile) {
        val base = project.basePath ?: return
        val path = vFile.path
        if (!path.startsWith(base)) return
        
        val relative = path.removePrefix(base).trimStart('/')
        val noteContent = service.getNoteForFile(relative)
        
        onFileChanged(relative)
        
        if (noteContent.isBlank()) {
            textArea.text = if (noteContent.isBlank()) "# 该文件暂无批注\n\n请直接在此编辑批注内容，支持 Markdown 格式。\n\n## 示例\n\n- 可以插入类图\n- 解释类核心方法\n- 标注类遗留事项\n- [md语法使用跳转](https://markdown.com.cn/basic-syntax/images.html)" else noteContent
        } else {
            textArea.text = noteContent
        }
    }
}
