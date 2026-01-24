package com.xandy.lite.models.encryptor

import com.xandy.lite.BuildConfig
import com.xandy.lite.models.encryptor.to.strings.String

private const val PART_ONE = BuildConfig.PART_ONE
private const val PART_TWO = BuildConfig.PART_TWO
private const val PART_THREE = BuildConfig.PART_THREE
private const val PART_FOUR = BuildConfig.PART_FOUR
private const val PART_FIVE = BuildConfig.PART_FIVE

internal object PW {
    val one = String(PART_ONE)
    val two = String(PART_TWO)
    val three = String(PART_THREE)
    val four = String(PART_FOUR)
    val five = String(PART_FIVE)
}