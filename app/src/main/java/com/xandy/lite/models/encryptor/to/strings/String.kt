package com.xandy.lite.models.encryptor.to.strings

import kotlin.String

class String(private val value: String) {
    override fun toString(): String = "****"
    fun get() = value
}