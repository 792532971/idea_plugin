package com.github.`792532971`.ideaplugin.naming

enum class NamingStyle {
    CAMEL_CASE {
        override fun displayName() = "camelCase"
        override fun convert(words: List<String>): String {
            require(words.isNotEmpty()) { "words list must not be empty" }
            return words.first() + words.drop(1).joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
        }
    },
    SNAKE_CASE {
        override fun displayName() = "snake_case"
        override fun convert(words: List<String>): String {
            require(words.isNotEmpty()) { "words list must not be empty" }
            return words.joinToString("_")
        }
    },
    PASCAL_CASE {
        override fun displayName() = "PascalCase"
        override fun convert(words: List<String>): String {
            require(words.isNotEmpty()) { "words list must not be empty" }
            return words.joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
        }
    },
    SCREAMING_SNAKE_CASE {
        override fun displayName() = "SCREAMING_SNAKE_CASE"
        override fun convert(words: List<String>): String {
            require(words.isNotEmpty()) { "words list must not be empty" }
            return words.joinToString("_") { it.uppercase() }
        }
    };

    abstract fun convert(words: List<String>): String
    abstract fun displayName(): String

    companion object {
        fun fromDisplayName(name: String): NamingStyle? {
            return entries.find { it.displayName() == name }
        }

        fun default(): NamingStyle = CAMEL_CASE
    }
}
