package com.github.`792532971`.ideaplugin.ui

import com.github.`792532971`.ideaplugin.naming.NamingStyle
import com.github.`792532971`.ideaplugin.services.AiNamingService
import com.github.`792532971`.ideaplugin.settings.PluginSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import java.awt.GridBagConstraints
import java.awt.GridBagConstraints.BOTH
import java.awt.GridBagConstraints.HORIZONTAL
import java.awt.GridBagConstraints.LINE_START
import java.awt.GridBagConstraints.REMAINDER
import java.awt.GridBagConstraints.VERTICAL
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.SwingWorker

class SettingsPanel(private val project: Project) : JPanel(GridBagLayout()) {

    private val providerNameField = JBTextField()
    private val apiKeyField = JBPasswordField()
    private val baseUrlField = JBTextField().apply {
        emptyText.text = "兼容 Claude API，不以 / 结尾"
    }
    private val modelNameField = JBTextField()
    private val temperatureField = JBTextField("0.2")
    private val namingStyleGroup = ButtonGroup()
    private val namingStyleButtons = mutableListOf<JRadioButton>()
    private val statusLabel = JLabel(" ")
    private val saveButton = JButton("保存")
    private val testButton = JButton("测试连接")

    private val settings: PluginSettings get() = PluginSettings.getInstance(project)

    init {
        val gbc = GridBagConstraints().apply {
            insets = Insets(4, 4, 4, 4)
            anchor = LINE_START
        }

        // Row 0: 供应商名称
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; gbc.fill = HORIZONTAL
        add(JBLabel("供应商名称"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        add(providerNameField, gbc)

        // Row 1: API Key
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0
        add(JBLabel("API Key"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        add(apiKeyField, gbc)

        // Row 2: 请求地址
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0
        add(JBLabel("请求地址"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        add(baseUrlField, gbc)

        // Row 3: 模型名称
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0
        add(JBLabel("模型名称"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        add(modelNameField, gbc)

        // Row 4: Temperature
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0
        add(JBLabel("Temperature"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        add(temperatureField, gbc)

        // Row 5: 默认命名风格 (span two columns)
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.weightx = 1.0
        add(JBLabel("默认命名风格"), gbc)
        gbc.gridwidth = 1

        // Row 6: Vertical radio buttons
        val namingPanel = JPanel(GridBagLayout()).apply {
            val innerGbc = GridBagConstraints().apply {
                gridx = 0
                anchor = LINE_START
                fill = HORIZONTAL
                weightx = 1.0
                insets = Insets(2, 0, 2, 0)
            }
            NamingStyle.entries.forEachIndexed { index, style ->
                val button = JRadioButton(style.displayName()).apply {
                    isSelected = style == settings.defaultNamingStyle
                }
                namingStyleGroup.add(button)
                namingStyleButtons.add(button)
                innerGbc.gridy = index
                add(button, innerGbc)
            }
        }
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.fill = HORIZONTAL
        add(namingPanel, gbc)
        gbc.gridwidth = 1

        // Row 7: Buttons
        val buttonPanel = JPanel().apply {
            add(saveButton)
            add(testButton)
        }
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.fill = HORIZONTAL
        add(buttonPanel, gbc)
        gbc.gridwidth = 1

        // Row 8: Status label
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 1.0; gbc.fill = BOTH
        add(statusLabel, gbc)

        // Listeners
        saveButton.addActionListener { saveSettings() }
        testButton.addActionListener { onTestConnection() }

        loadSettings()
    }

    fun loadSettings() {
        providerNameField.text = settings.providerName
        apiKeyField.text = settings.apiKey
        baseUrlField.text = settings.baseUrl
        modelNameField.text = settings.modelName
        temperatureField.text = settings.temperature.toString()
        namingStyleButtons.forEach { button ->
            button.isSelected = button.text == settings.defaultNamingStyle.displayName()
        }
    }

    fun saveSettings() {
        settings.providerName = providerNameField.text
        settings.apiKey = String(apiKeyField.password)
        settings.baseUrl = baseUrlField.text
        settings.modelName = modelNameField.text
        settings.temperature = temperatureField.text.toDoubleOrNull() ?: 0.2
        val selectedStyle = namingStyleButtons.find { it.isSelected }?.text ?: ""
        settings.defaultNamingStyle = NamingStyle.fromDisplayName(selectedStyle) ?: NamingStyle.default()
    }

    private fun onTestConnection() {
        saveSettings()
        if (!settings.isConfigured()) {
            statusLabel.text = "请先配置请求地址、API Key 和模型名称"
            return
        }
        statusLabel.text = "正在测试连接..."
        object : SwingWorker<String, Void>() {
            override fun doInBackground(): String {
                val service = AiNamingService(
                    settings.baseUrl,
                    settings.apiKey,
                    settings.modelName,
                    settings.temperature
                )
                return service.testConnection()
            }

            override fun done() {
                try {
                    statusLabel.text = get()
                } catch (e: Exception) {
                    statusLabel.text = "连接失败: ${e.cause?.message ?: e.message}"
                }
            }
        }.execute()
    }
}
