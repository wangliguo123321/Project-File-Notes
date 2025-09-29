package com.projectfilenotes.ui

import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

class NoteFileListCellRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
    ): Component {
        val comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        if (value is NoteFileItem) {
            text = value.filePath
            toolTipText = value.content.take(200) + if (value.content.length > 200) "..." else ""
        }
        return comp
    }
}
