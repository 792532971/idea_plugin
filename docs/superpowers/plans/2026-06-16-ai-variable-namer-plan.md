# AI Variable Namer Plugin Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a PhpStorm 2026.1 plugin with a side Tool Window that converts Chinese descriptions into English variable names using a Claude-compatible LLM endpoint, supporting four naming styles.

**Architecture:** The plugin uses a single Tool Window with two tabs. A `PersistentStateComponent` stores LLM settings. An `AiNamingService` calls the Claude Messages API via OkHttp. A `NamingStyle` enum converts English word lists into the selected casing. The UI is built with plain Swing/JPanel to keep dependencies minimal and avoid UI DSL complexity.

**Tech Stack:** Kotlin 1.9.24, IntelliJ Platform Gradle Plugin 2.16.0, OkHttp 4.12.0, Gson 2.10.1, PhpStorm 2026.1 target platform.

---

## File Structure

### New Files
- `src/main/kotlin/com/github/792532971/ideaplugin/naming/NamingStyle.kt` — Enum for naming styles and conversion logic.
- `src/main/kotlin/com/github/792532971/ideaplugin/settings/NamingSettings.kt` — Persistent state data class.
- `src/main/kotlin/com/github/792532971/ideaplugin/settings/PluginSettings.kt` — `PersistentStateComponent` implementation.
- `src/main/kotlin/com/github/792532971/ideaplugin/services/AiNamingService.kt` — HTTP client for Claude-compatible API.
- `src/main/kotlin/com/github/792532971/ideaplugin/ui/GeneratePanel.kt` — Generate tab UI.
- `src/main/kotlin/com/github/792532971/ideaplugin/ui/SettingsPanel.kt` — Settings tab UI.
- `src/main/kotlin/com/github/792532971/ideaplugin/toolWindow/VariableNamerToolWindowFactory.kt` — Tool Window factory that hosts both tabs.
- `src/test/kotlin/com/github/792532971/ideaplugin/naming/NamingStyleTest.kt` — Unit tests for naming conversion.
- `src/test/kotlin/com/github/792532971/ideaplugin/services/AiNamingServiceTest.kt` — Unit tests for response parsing.

### Modified Files
- `src/main/resources/META-INF/plugin.xml` — Register new Tool Window and remove sample Tool Window.
- `build.gradle.kts` — Ensure target platform is `phpstorm("2026.1")`, fix malformed `intellijPlatform` block.
- `src/main/resources/messages/MyBundle.properties` — Add UI labels.
- `src/main/kotlin/com/github/792532971/ideaplugin/toolWindow/MyToolWindowFactory.kt` — Delete (sample code).
- `src/main/kotlin/com/github/792532971/ideaplugin/services/MyProjectService.kt` — Delete if unused after removing sample.
- `src/main/kotlin/com/github/792532971/ideaplugin/startup/MyProjectActivity.kt` — Delete (sample code).

---

## Task 1: Fix Build Configuration

**Files:**
- Modify: `build.gradle.kts`

**Context:** Current `build.gradle.kts` has two issues: dependencies are placed inside the `intellijPlatform` block incorrectly (they belong in `dependencies {}`), and the target platform should match PhpStorm 2026.1.

- [ ] **Step 1: Rewrite build.gradle.kts**

```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.24"
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog") version "2.2.1"
}

group = "com.github.792532971.ideaplugin"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    intellijPlatform {
        phpstorm("2026.1")
        bundledPlugin("com.jetbrains.php")
        testFramework(TestFrameworkType.Platform)
    }

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}
```

- [ ] **Step 2: Verify Gradle sync**

Run: `./gradlew.bat help` (Windows)
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add build.gradle.kts
git commit -m "build: fix intellijPlatform DSL and target PhpStorm 2026.1"
```

---

## Task 2: Define NamingStyle Enum

**Files:**
- Create: `src/main/kotlin/com/github/792532971/ideaplugin/naming/NamingStyle.kt`
- Test: `src/test/kotlin/com/github/792532971/ideaplugin/naming/NamingStyleTest.kt`

**Context:** This enum converts a list of lower-case English words into the selected variable name casing.

- [ ] **Step 1: Write the failing test**

Create `src/test/kotlin/com/github/792532971/ideaplugin/naming/NamingStyleTest.kt`:

```kotlin
package com.github.792532971.ideaplugin.naming

import org.junit.Test
import kotlin.test.assertEquals

class NamingStyleTest {

    @Test
    fun `camelCase combines words`() {
        assertEquals("userOrderList", NamingStyle.CAMEL_CASE.convert(listOf("user", "order", "list")))
    }

    @Test
    fun `snake_case combines words`() {
        assertEquals("user_order_list", NamingStyle.SNAKE_CASE.convert(listOf("user", "order", "list")))
    }

    @Test
    fun `PascalCase combines words`() {
        assertEquals("UserOrderList", NamingStyle.PASCAL_CASE.convert(listOf("user", "order", "list")))
    }

    @Test
    fun `SCREAMING_SNAKE_CASE combines words`() {
        assertEquals("USER_ORDER_LIST", NamingStyle.SCREAMING_SNAKE_CASE.convert(listOf("user", "order", "list")))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew.bat test --tests "com.github.792532971.ideaplugin.naming.NamingStyleTest"`
Expected: Compilation failure because `NamingStyle` does not exist.

- [ ] **Step 3: Implement NamingStyle**

Create `src/main/kotlin/com/github/792532971/ideaplugin/naming/NamingStyle.kt`:

```kotlin
package com.github.792532971.ideaplugin.naming

enum class NamingStyle {
    CAMEL_CASE,
    SNAKE_CASE,
    PASCAL_CASE,
    SCREAMING_SNAKE_CASE;

    fun convert(words: List<String>): String {
        require(words.isNotEmpty()) { "Word list must not be empty" }
        val normalized = words.map { it.lowercase().replaceFirstChar { c -> c.titlecase() } }
        return when (this) {
            CAMEL_CASE -> normalized.first().lowercase() + normalized.drop(1).joinToString("")
            SNAKE_CASE -> words.joinToString("_") { it.lowercase() }
            PASCAL_CASE -> normalized.joinToString("")
            SCREAMING_SNAKE_CASE -> words.joinToString("_") { it.uppercase() }
        }
    }

    companion object {
        fun fromDisplayName(name: String): NamingStyle? =
            entries.find { it.displayName() == name }

        fun default(): NamingStyle = CAMEL_CASE
    }

    fun displayName(): String = when (this) {
        CAMEL_CASE -> "camelCase"
        SNAKE_CASE -> "snake_case"
        PASCAL_CASE -> "PascalCase"
        SCREAMING_SNAKE_CASE -> "SCREAMING_SNAKE_CASE"
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew.bat test --tests "com.github.792532971.ideaplugin.naming.NamingStyleTest"`
Expected: BUILD SUCCESSFUL, tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/github/792532971/ideaplugin/naming/NamingStyle.kt \
        src/test/kotlin/com/github/792532971/ideaplugin/naming/NamingStyleTest.kt
git commit -m "feat: add NamingStyle enum with conversion logic"
```

---

## Task 3: Implement Settings Persistence

**Files:**
- Create: `src/main/kotlin/com/github/792532971/ideaplugin/settings/NamingSettings.kt`
- Create: `src/main/kotlin/com/github/792532971/ideaplugin/settings/PluginSettings.kt`

**Context:** Plugin settings are stored via `PersistentStateComponent` at project level.

- [ ] **Step 1: Create data class**

Create `src/main/kotlin/com/github/792532971/ideaplugin/settings/NamingSettings.kt`:

```kotlin
package com.github.792532971.ideaplugin.settings

import com.github.792532971.ideaplugin.naming.NamingStyle

/**
 * Serializable settings state. All fields must have default values
 * so deserialization does not fail when fields are missing.
 */
data class NamingSettings(
    var providerName: String = "",
    var apiKey: String = "",
    var baseUrl: String = "",
    var modelName: String = "",
    var temperature: Double = 0.2,
    var defaultNamingStyle: String = NamingStyle.CAMEL_CASE.displayName()
)
```

- [ ] **Step 2: Implement PersistentStateComponent**

Create `src/main/kotlin/com/github/792532971/ideaplugin/settings/PluginSettings.kt`:

```kotlin
package com.github.792532971.ideaplugin.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.service
import com.github.792532971.ideaplugin.naming.NamingStyle

@Service(Service.Level.PROJECT)
@State(
    name = "AiVariableNamerSettings",
    storages = [Storage("AiVariableNamerSettings.xml")]
)
class PluginSettings : PersistentStateComponent<NamingSettings> {

    private var state = NamingSettings()

    override fun getState(): NamingSettings = state

    override fun loadState(state: NamingSettings) {
        this.state = state
    }

    var providerName: String
        get() = state.providerName
        set(value) { state.providerName = value }

    var apiKey: String
        get() = state.apiKey
        set(value) { state.apiKey = value }

    var baseUrl: String
        get() = state.baseUrl
        set(value) { state.baseUrl = value }

    var modelName: String
        get() = state.modelName
        set(value) { state.modelName = value }

    var temperature: Double
        get() = state.temperature
        set(value) { state.temperature = value }

    var defaultNamingStyle: NamingStyle
        get() = NamingStyle.fromDisplayName(state.defaultNamingStyle) ?: NamingStyle.default()
        set(value) { state.defaultNamingStyle = value.displayName() }

    fun isConfigured(): Boolean = baseUrl.isNotBlank() && apiKey.isNotBlank() && modelName.isNotBlank()

    companion object {
        fun getInstance(project: Project): PluginSettings = project.service()
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/github/792532971/ideaplugin/settings/
git commit -m "feat: add persistent plugin settings"
```

---

## Task 4: Implement AI Naming Service

**Files:**
- Create: `src/main/kotlin/com/github/792532971/ideaplugin/services/AiNamingService.kt`
- Test: `src/test/kotlin/com/github/792532971/ideaplugin/services/AiNamingServiceTest.kt`

**Context:** The service calls a Claude-compatible Messages API. It extracts English words from the response content and returns them as a list. No UI code belongs here.

- [ ] **Step 1: Write the failing test**

Create `src/test/kotlin/com/github/792532971/ideaplugin/services/AiNamingServiceTest.kt`:

```kotlin
package com.github.792532971.ideaplugin.services

import org.junit.Test
import kotlin.test.assertEquals

class AiNamingServiceTest {

    @Test
    fun `parseWords extracts space separated words`() {
        val service = AiNamingService("https://api.example.com", "sk-key", "model", 0.2)
        assertEquals(
            listOf("user", "order", "list"),
            service.parseWords("user order list")
        )
    }

    @Test
    fun `parseWords cleans punctuation and extra whitespace`() {
        val service = AiNamingService("https://api.example.com", "sk-key", "model", 0.2)
        assertEquals(
            listOf("user", "order", "list"),
            service.parseWords("  user, order; list.  ")
        )
    }

    @Test
    fun `parseWords filters empty tokens`() {
        val service = AiNamingService("https://api.example.com", "sk-key", "model", 0.2)
        assertEquals(
            listOf("user", "order"),
            service.parseWords("user   order")
        )
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew.bat test --tests "com.github.792532971.ideaplugin.services.AiNamingServiceTest"`
Expected: Compilation failure.

- [ ] **Step 3: Implement AiNamingService**

Create `src/main/kotlin/com/github/792532971/ideaplugin/services/AiNamingService.kt`:

```kotlin
package com.github.792532971.ideaplugin.services

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class AiNamingService(
    private val baseUrl: String,
    private val apiKey: String,
    private val modelName: String,
    private val temperature: Double
) {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class, IllegalStateException::class)
    fun generateEnglishWords(chineseDescription: String): List<String> {
        val body = buildRequestBody(chineseDescription)
        val request = Request.Builder()
            .url("$baseUrl/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("API request failed: ${response.code} ${response.message}")
            }
            val json = response.body?.string() ?: throw IOException("Empty response body")
            val content = extractContent(json)
            return parseWords(content)
        }
    }

    @Throws(IOException::class, IllegalStateException::class)
    fun testConnection(): String {
        val body = buildRequestBody("hello")
        val request = Request.Builder()
            .url("$baseUrl/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("API request failed: ${response.code} ${response.message}")
            }
            return "Connection successful"
        }
    }

    internal fun buildRequestBody(description: String): String {
        val json = JsonObject().apply {
            addProperty("model", modelName)
            addProperty("max_tokens", 64)
            addProperty("temperature", temperature)
            add("system", gson.toJsonTree("You are a coding assistant. Translate the Chinese description into 2-5 concise English words suitable for a variable name. Return ONLY the words, space-separated, lowercase. No explanation, no punctuation."))
            add("messages", gson.toJsonTree(listOf(
                mapOf("role" to "user", "content" to description)
            )))
        }
        return gson.toJson(json)
    }

    internal fun extractContent(responseJson: String): String {
        val root = gson.fromJson(responseJson, JsonObject::class.java)
        val contentArray = root.getAsJsonArray("content")
            ?: throw IllegalStateException("Missing 'content' array in response")
        if (contentArray.size() == 0) {
            throw IllegalStateException("Empty 'content' array in response")
        }
        val text = contentArray[0].asJsonObject.get("text")?.asString
            ?: throw IllegalStateException("Missing 'text' field in response content")
        return text
    }

    fun parseWords(text: String): List<String> {
        return text
            .replace(Regex("[^A-Za-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .map { it.lowercase() }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew.bat test --tests "com.github.792532971.ideaplugin.services.AiNamingServiceTest"`
Expected: BUILD SUCCESSFUL, tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/github/792532971/ideaplugin/services/AiNamingService.kt \
        src/test/kotlin/com/github/792532971/ideaplugin/services/AiNamingServiceTest.kt
git commit -m "feat: add Claude-compatible AI naming service"
```

---

## Task 5: Implement Settings Panel UI

**Files:**
- Create: `src/main/kotlin/com/github/792532971/ideaplugin/ui/SettingsPanel.kt`

**Context:** Settings panel edits the `PluginSettings` state and provides Save / Test Connection actions. It runs on the Swing EDT.

- [ ] **Step 1: Implement SettingsPanel**

Create `src/main/kotlin/com/github/792532971/ideaplugin/ui/SettingsPanel.kt`:

```kotlin
package com.github.792532971.ideaplugin.ui

import com.github.792532971.ideaplugin.naming.NamingStyle
import com.github.792532971.ideaplugin.services.AiNamingService
import com.github.792532971.ideaplugin.settings.PluginSettings
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class SettingsPanel(private val project: Project) : JPanel(GridBagLayout()) {

    private val settings = PluginSettings.getInstance(project)

    private val providerField = JBTextField()
    private val apiKeyField = JBPasswordField()
    private val baseUrlField = JBTextField()
    private val modelField = JBTextField()
    private val temperatureField = JBTextField("0.2")
    private val styleGroup = ButtonGroup()
    private val statusLabel = JBLabel(" ")

    init {
        val constraints = GridBagConstraints().apply {
            insets = Insets(6, 6, 6, 6)
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.WEST
        }

        addLabeledField("供应商名称", providerField, constraints, 0)
        addLabeledField("API Key", apiKeyField, constraints, 1)
        addLabeledField("请求地址", baseUrlField, constraints, 2)
        addLabeledField("模型名称", modelField, constraints, 3)
        addLabeledField("Temperature", temperatureField, constraints, 4)

        constraints.gridx = 0
        constraints.gridy = 5
        constraints.gridwidth = 2
        add(JBLabel("默认命名风格"), constraints)

        val stylePanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            NamingStyle.entries.forEach { style ->
                val radio = JRadioButton(style.displayName(), style == settings.defaultNamingStyle)
                radio.actionCommand = style.displayName()
                styleGroup.add(radio)
                add(radio)
            }
        }
        constraints.gridy = 6
        add(stylePanel, constraints)

        val buttonPanel = JPanel().apply {
            add(JButton("保存").apply { addActionListener { saveSettings() } })
            add(JButton("测试连接").apply { addActionListener { testConnection() } })
        }
        constraints.gridy = 7
        add(buttonPanel, constraints)

        constraints.gridy = 8
        constraints.weighty = 1.0
        constraints.anchor = GridBagConstraints.NORTHWEST
        add(statusLabel, constraints)

        loadSettings()
    }

    private fun addLabeledField(label: String, field: JComponent, constraints: GridBagConstraints, row: Int) {
        constraints.gridx = 0
        constraints.gridy = row
        constraints.gridwidth = 1
        constraints.weightx = 0.0
        add(JBLabel(label), constraints)

        constraints.gridx = 1
        constraints.weightx = 1.0
        add(field, constraints)
    }

    private fun loadSettings() {
        providerField.text = settings.providerName
        apiKeyField.text = settings.apiKey
        baseUrlField.text = settings.baseUrl
        modelField.text = settings.modelName
        temperatureField.text = settings.temperature.toString()
        styleGroup.elements.toList().find { it.actionCommand == settings.defaultNamingStyle.displayName() }?.isSelected = true
    }

    private fun saveSettings() {
        settings.providerName = providerField.text.trim()
        settings.apiKey = String(apiKeyField.password)
        settings.baseUrl = baseUrlField.text.trim()
        settings.modelName = modelField.text.trim()
        settings.temperature = temperatureField.text.toDoubleOrNull() ?: 0.2
        settings.defaultNamingStyle = NamingStyle.fromDisplayName(styleGroup.selection?.actionCommand) ?: NamingStyle.default()
        statusLabel.text = "设置已保存"
    }

    private fun testConnection() {
        saveSettings()
        if (!settings.isConfigured()) {
            statusLabel.text = "请先填写请求地址、API Key 和模型名称"
            return
        }

        statusLabel.text = "测试中..."
        SwingWorker<String, Void>() {
            override fun doInBackground(): String {
                val service = AiNamingService(settings.baseUrl, settings.apiKey, settings.modelName, settings.temperature)
                return service.testConnection()
            }

            override fun done() {
                statusLabel.text = try { get() } catch (e: Exception) { "连接失败: ${e.cause?.message ?: e.message}" }
            }
        }.execute()
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/kotlin/com/github/792532971/ideaplugin/ui/SettingsPanel.kt
git commit -m "feat: add settings panel UI"
```

---

## Task 6: Implement Generate Panel UI

**Files:**
- Create: `src/main/kotlin/com/github/792532971/ideaplugin/ui/GeneratePanel.kt`

**Context:** Generate panel is the main interaction surface. It uses `AiNamingService` and `NamingStyle` to produce a variable name.

- [ ] **Step 1: Implement GeneratePanel**

Create `src/main/kotlin/com/github/792532971/ideaplugin/ui/GeneratePanel.kt`:

```kotlin
package com.github.792532971.ideaplugin.ui

import com.github.792532971.ideaplugin.naming.NamingStyle
import com.github.792532971.ideaplugin.services.AiNamingService
import com.github.792532971.ideaplugin.settings.PluginSettings
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import javax.swing.*

class GeneratePanel(private val project: Project) : JPanel(BorderLayout()) {

    private val settings = PluginSettings.getInstance(project)

    private val inputArea = JBTextArea(4, 30).apply {
        lineWrap = true
        wrapStyleWord = true
    }
    private val styleGroup = ButtonGroup()
    private val resultField = JBTextField().apply { isEditable = false }
    private val statusLabel = JBLabel(" ")

    init {
        val northPanel = JPanel(BorderLayout()).apply {
            add(JBLabel("中文描述"), BorderLayout.NORTH)
            add(JScrollPane(inputArea), BorderLayout.CENTER)
        }
        add(northPanel, BorderLayout.NORTH)

        val centerPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = Component.LEFT_ALIGNMENT

            add(JBLabel("命名风格").apply { alignmentX = Component.LEFT_ALIGNMENT })
            val styleBox = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                NamingStyle.entries.forEach { style ->
                    val radio = JRadioButton(style.displayName(), style == settings.defaultNamingStyle)
                    radio.actionCommand = style.displayName()
                    styleGroup.add(radio)
                    styleBox.add(radio)
                }
                alignmentX = Component.LEFT_ALIGNMENT
            }
            add(styleBox)

            add(Box.createVerticalStrut(12))

            val generateButton = JButton("生成变量名").apply {
                alignmentX = Component.LEFT_ALIGNMENT
                addActionListener { generateName() }
            }
            add(generateButton)

            add(Box.createVerticalStrut(12))

            add(JBLabel("生成结果").apply { alignmentX = Component.LEFT_ALIGNMENT })
            add(resultField.apply { maximumSize = Dimension(Int.MAX_VALUE, 30); alignmentX = Component.LEFT_ALIGNMENT })

            add(Box.createVerticalStrut(8))

            val copyButton = JButton("复制到剪贴板").apply {
                alignmentX = Component.LEFT_ALIGNMENT
                addActionListener { copyResult() }
            }
            add(copyButton)
        }
        add(centerPanel, BorderLayout.CENTER)
        add(statusLabel, BorderLayout.SOUTH)
    }

    private fun generateName() {
        val description = inputArea.text.trim()
        if (description.isEmpty()) {
            statusLabel.text = "请输入中文描述"
            return
        }
        if (!settings.isConfigured()) {
            statusLabel.text = "请先在「设置」标签页配置 API"
            return
        }

        statusLabel.text = "生成中..."
        val selectedStyle = NamingStyle.fromDisplayName(styleGroup.selection?.actionCommand) ?: NamingStyle.default()

        SwingWorker<String, Void>() {
            override fun doInBackground(): String {
                val service = AiNamingService(settings.baseUrl, settings.apiKey, settings.modelName, settings.temperature)
                val words = service.generateEnglishWords(description)
                return selectedStyle.convert(words)
            }

            override fun done() {
                try {
                    resultField.text = get()
                    statusLabel.text = " "
                } catch (e: Exception) {
                    resultField.text = ""
                    statusLabel.text = "生成失败: ${e.cause?.message ?: e.message}"
                }
            }
        }.execute()
    }

    private fun copyResult() {
        val text = resultField.text
        if (text.isNotEmpty()) {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
            statusLabel.text = "已复制"
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/kotlin/com/github/792532971/ideaplugin/ui/GeneratePanel.kt
git commit -m "feat: add generate panel UI"
```

---

## Task 7: Implement Tool Window Factory

**Files:**
- Create: `src/main/kotlin/com/github/792532971/ideaplugin/toolWindow/VariableNamerToolWindowFactory.kt`

**Context:** This factory registers the Tool Window and hosts both tabs via a JTabbedPane.

- [ ] **Step 1: Implement factory**

Create `src/main/kotlin/com/github/792532971/ideaplugin/toolWindow/VariableNamerToolWindowFactory.kt`:

```kotlin
package com.github.792532971.ideaplugin.toolWindow

import com.github.792532971.ideaplugin.ui.GeneratePanel
import com.github.792532971.ideaplugin.ui.SettingsPanel
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JTabbedPane

class VariableNamerToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val tabbedPane = JTabbedPane().apply {
            addTab("生成", GeneratePanel(project))
            addTab("设置", SettingsPanel(project))
        }

        val content = ContentFactory.getInstance().createContent(tabbedPane, "", false)
        content.isCloseable = false
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/kotlin/com/github/792532971/ideaplugin/toolWindow/VariableNamerToolWindowFactory.kt
git commit -m "feat: add variable namer tool window factory"
```

---

## Task 8: Register Plugin and Clean Up Sample Code

**Files:**
- Modify: `src/main/resources/META-INF/plugin.xml`
- Modify: `src/main/resources/messages/MyBundle.properties`
- Delete: `src/main/kotlin/com/github/792532971/ideaplugin/toolWindow/MyToolWindowFactory.kt`
- Delete: `src/main/kotlin/com/github/792532971/ideaplugin/services/MyProjectService.kt`
- Delete: `src/main/kotlin/com/github/792532971/ideaplugin/startup/MyProjectActivity.kt`
- Delete: `src/test/kotlin/com/github/792532971/ideaplugin/MyPluginTest.kt` (sample test)

**Context:** Replace sample registrations with the new Tool Window and remove unused sample files.

- [ ] **Step 1: Update plugin.xml**

Replace the content of `src/main/resources/META-INF/plugin.xml` with:

```xml
<!-- Plugin Configuration File. Read more: https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html -->
<idea-plugin>
    <id>com.github.792532971.ideaplugin</id>
    <name>AI Variable Namer</name>
    <vendor>792532971</vendor>
    <description><![CDATA[
        <p><b>AI Variable Namer</b> generates English variable names from Chinese descriptions using a Claude-compatible LLM endpoint.</p>
        <p>Supports camelCase, snake_case, PascalCase, and SCREAMING_SNAKE_CASE.</p>
    ]]]></description>

    <depends>com.intellij.modules.platform</depends>

    <resource-bundle>messages.MyBundle</resource-bundle>

    <extensions defaultExtensionNs="com.intellij">
        <toolWindow factoryClass="com.github.792532971.ideaplugin.toolWindow.VariableNamerToolWindowFactory" id="AI Variable Namer" anchor="right" canCloseContents="false"/>
    </extensions>
</idea-plugin>
```

- [ ] **Step 2: Update bundle properties**

Replace `src/main/resources/messages/MyBundle.properties` with:

```properties
# Placeholder — UI strings are currently inline in Kotlin panels.
# This file remains because plugin.xml references it as the resource bundle.
```

- [ ] **Step 3: Remove sample files**

Delete these files:
- `src/main/kotlin/com/github/792532971/ideaplugin/toolWindow/MyToolWindowFactory.kt`
- `src/main/kotlin/com/github/792532971/ideaplugin/services/MyProjectService.kt`
- `src/main/kotlin/com/github/792532971/ideaplugin/startup/MyProjectActivity.kt`
- `src/test/kotlin/com/github/792532971/ideaplugin/MyPluginTest.kt`

- [ ] **Step 4: Build to verify**

Run: `./gradlew.bat build`
Expected: BUILD SUCCESSFUL. There may be warnings about unused properties or empty bundle; these are acceptable.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/META-INF/plugin.xml \
        src/main/resources/messages/MyBundle.properties \
        src/main/kotlin/com/github/792532971/ideaplugin/toolWindow/VariableNamerToolWindowFactory.kt
git rm src/main/kotlin/com/github/792532971/ideaplugin/toolWindow/MyToolWindowFactory.kt \
        src/main/kotlin/com/github/792532971/ideaplugin/services/MyProjectService.kt \
        src/main/kotlin/com/github/792532971/ideaplugin/startup/MyProjectActivity.kt \
        src/test/kotlin/com/github/792532971/ideaplugin/MyPluginTest.kt
git commit -m "feat: register AI Variable Namer tool window and remove sample code"
```

---

## Task 9: Final Verification

- [ ] **Step 1: Run all unit tests**

Run: `./gradlew.bat test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Run plugin in PhpStorm sandbox**

Run: `./gradlew.bat runIde`
Expected: PhpStorm sandbox launches. Open the Tool Window from the right side.

- [ ] **Step 3: Manual smoke test checklist**
- [ ] Tool Window shows two tabs: 生成 / 设置.
- [ ] Settings tab saves values and persists across IDE restart.
- [ ] Test Connection button returns success/failure message.
- [ ] Generate tab produces a variable name when configured.
- [ ] Copy button writes result to clipboard.

- [ ] **Step 4: Commit any fixes**

```bash
git add -A
git commit -m "fix: address final verification issues"
```

---

## Spec Coverage Checklist

| Spec Requirement | Task |
|------------------|------|
| Side Tool Window with two tabs | Task 7 |
| Generate tab UI | Task 6 |
| Settings tab UI | Task 5 |
| Four naming styles | Task 2 |
| Claude-compatible API call | Task 4 |
| Settings persistence | Task 3 |
| Error handling in UI | Tasks 5, 6 |
| Target PhpStorm 2026.1 | Task 1 |
| Copy to clipboard | Task 6 |
| Test connection | Task 5 |

## Placeholder Scan

No TBD, TODO, or vague steps present. Every code block is complete and every command includes expected output.

## Type Consistency Notes

- `NamingStyle.displayName()` is used throughout. Default style stored as display name string in `NamingSettings`.
- `PluginSettings.isConfigured()` checks `baseUrl`, `apiKey`, and `modelName`.
- `AiNamingService` constructor parameters match fields read from `PluginSettings`.
