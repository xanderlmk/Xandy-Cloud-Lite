package com.xandy.lite.models.encryptor.to.strings
import kotlin.String
import kotlin.Char
class Char(private val value: Char) {
    override fun toString(): String = "*"
    fun get() = value
}