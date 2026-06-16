package com.github.`792532971`.ideaplugin.settings

import com.github.`792532971`.ideaplugin.naming.NamingStyle

/**
 * Persistent data class for plugin settings.
 */
data class NamingSettings(
    var providerName: String = "",
    var apiKey: String = "",
    var baseUrl: String = "",
    var modelName: String = "",
    var temperature: Double = 0.2,
    var defaultNamingStyle: String = NamingStyle.CAMEL_CASE.displayName()
)
