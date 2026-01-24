package com.xandy.lite.models.ui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@SerialName("XCLanguage")
enum class XCLanguage {
    English, Spanish, Default;

    fun toLocale() = when (this) {
        English -> "en"
        Spanish -> "es"
        Default -> null
    }
    fun isEnglish() = this == English
    fun isSpanish() = this == Spanish
    fun isDefault() = this == Default
    override fun toString(): String {
        return when(this) {
            English -> "English"
            Spanish -> "Español"
            Default -> ""
        }
    }

}