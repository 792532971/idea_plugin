package com.github.`792532971`.ideaplugin.toolWindow

import com.github.`792532971`.ideaplugin.ui.GeneratePanel
import com.github.`792532971`.ideaplugin.ui.SettingsPanel
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JTabbedPane

class VariableNamerToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val tabbedPane = JTabbedPane()
        tabbedPane.addTab("生成", GeneratePanel(project))
        tabbedPane.addTab("设置", SettingsPanel(project))

        val content = ContentFactory.getInstance().createContent(tabbedPane, "", false)
        content.isCloseable = false
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true
}
