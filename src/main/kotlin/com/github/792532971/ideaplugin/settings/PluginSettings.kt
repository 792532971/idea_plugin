package com.github.`792532971`.ideaplugin.settings

import com.github.`792532971`.ideaplugin.naming.NamingStyle
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.service

/**
 * Project-level persistent settings for the AI Variable Namer plugin.
 */
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
        get() = NamingStyle.fromDisplayName(state.defaultNamingStyle) ?: NamingStyle.CAMEL_CASE
        set(value) { state.defaultNamingStyle = value.displayName() }

    fun isConfigured(): Boolean =
        baseUrl.isNotBlank() && apiKey.isNotBlank() && modelName.isNotBlank()

    companion object {
        fun getInstance(project: Project): PluginSettings = project.service()
    }
}
