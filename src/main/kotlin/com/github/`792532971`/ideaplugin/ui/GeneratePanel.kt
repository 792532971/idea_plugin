package com.github.`792532971`.ideaplugin.ui

import com.github.`792532971`.ideaplugin.naming.NamingStyle
import com.github.`792532971`.ideaplugin.services.AiNamingService
import com.github.`792532971`.ideaplugin.settings.PluginSettings
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JScrollPane
import javax.swing.SwingWorker

class GeneratePanel(private val project: Project) : JPanel(BorderLayout()) {

    private val descriptionArea = JBTextArea(4, 30)
    private val namingStyleGroup = ButtonGroup()
    private val namingStyleButtons = mutableListOf<JRadioButton>()
    private val generateButton = JButton("生成变量名")
    private val resultField = JBTextField().apply { isEditable = false }
    private val copyButton = JButton("复制到剪贴板")
    private val statusLabel = JBLabel(" ")

    private val settings: PluginSettings get() = PluginSettings.getInstance(project)

    init {
        // North: Chinese description label + text area in scroll pane
        val northPanel = JPanel(BorderLayout()).apply {
            add(JBLabel("中文描述"), BorderLayout.NORTH)
            add(JScrollPane(descriptionArea), BorderLayout.CENTER)
            border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
        }
        add(northPanel, BorderLayout.NORTH)

        // Center: vertical box with naming style, generate button, result, copy button
        val centerPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(4, 4, 4, 4)

            // Naming style label
            add(JBLabel("命名风格"))
            add(Box.createRigidArea(Dimension(0, 4)))

            // Vertical radio buttons
            NamingStyle.entries.forEach { style ->
                val button = JRadioButton(style.displayName()).apply {
                    isSelected = style == settings.defaultNamingStyle
                }
                namingStyleGroup.add(button)
                namingStyleButtons.add(button)
                add(button)
            }
            add(Box.createRigidArea(Dimension(0, 8)))

            // Generate button
            add(generateButton)
            add(Box.createRigidArea(Dimension(0, 8)))

            // Result label
            add(JBLabel("生成结果"))
            add(Box.createRigidArea(Dimension(0, 4)))

            // Result field
            add(resultField)
            add(Box.createRigidArea(Dimension(0, 8)))

            // Copy button
            add(copyButton)

            // Push everything to top
            add(Box.createVerticalGlue())
        }
        add(centerPanel, BorderLayout.CENTER)

        // South: Status label
        val southPanel = JPanel(BorderLayout()).apply {
            add(statusLabel, BorderLayout.CENTER)
            border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
        }
        add(southPanel, BorderLayout.SOUTH)

        // Listeners
        generateButton.addActionListener { onGenerate() }
        copyButton.addActionListener { onCopy() }
    }

    private fun onGenerate() {
        val input = descriptionArea.text.trim()
        if (input.isEmpty()) {
            statusLabel.text = "请输入中文描述"
            return
        }
        if (!settings.isConfigured()) {
            statusLabel.text = "请先配置请求地址、API Key 和模型名称"
            return
        }

        statusLabel.text = "正在生成..."
        generateButton.isEnabled = false

        val selectedStyle = namingStyleButtons.find { it.isSelected }?.let { button ->
            NamingStyle.entries.find { it.displayName() == button.text }
        } ?: NamingStyle.default()

        object : SwingWorker<String, Void>() {
            override fun doInBackground(): String {
                val service = AiNamingService(
                    settings.baseUrl,
                    settings.apiKey,
                    settings.modelName,
                    settings.temperature
                )
                val words = service.generateEnglishWords(input)
                return selectedStyle.convert(words)
            }

            override fun done() {
                generateButton.isEnabled = true
                try {
                    val result = get()
                    resultField.text = result
                    statusLabel.text = "生成成功"
                } catch (e: Exception) {
                    resultField.text = ""
                    statusLabel.text = "生成失败: ${e.cause?.message ?: e.message}"
                }
            }
        }.execute()
    }

    private fun onCopy() {
        val text = resultField.text
        if (text.isNotEmpty()) {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(text), null)
            statusLabel.text = "已复制到剪贴板"
        } else {
            statusLabel.text = "没有可复制的内容"
        }
    }
}
