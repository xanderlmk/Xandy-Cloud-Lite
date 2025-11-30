package com.xandy.lite.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.xandy.lite.models.Theme
import com.xandy.lite.models.application.PrefRepository
import com.xandy.lite.models.media.player.LoadControl
import com.xandy.lite.models.media.player.PlayerControls

@Suppress("unused")
class GetUIStyle(
    private val cS: ColorScheme, private val isDarkTheme: Boolean,
    private val isDynamicTheme: Boolean
) {
    fun getIsDarkTheme() = isDarkTheme
    fun floatingPlayerBackground(): Color =
        if (isDarkTheme) Color(33, 40, 40, 255)
        else Color(127, 141, 141, 255)

    private fun inactiveTabColor(): Color =
        if (isDarkTheme) Color(6, 33, 79, 255)
        else Color(68, 155, 196, 255)

    private fun activeTabColor(): Color =
        if (isDarkTheme) Color(1, 1, 66, 255)
        else Color(74, 239, 245, 255)

    fun tabColor(isSelected: Boolean): Color =
        if (isSelected) activeTabColor() else inactiveTabColor()

    fun tabTextColor(isSelected: Boolean): Color =
        if (isDarkTheme) if (isSelected) Color.Companion.White else Color.Companion.Gray
        else if (isSelected) Color.Companion.Black else Color.Companion.DarkGray

    fun topBarColor(): Color =
        if (isDarkTheme) Color(12, 11, 24, 255)
        else Color(162, 169, 253, 255)

    fun themedOnContainerColor(): Color =
        if (isDarkTheme) Color.Companion.White else Color.Companion.Black

    fun altThemedColor(): Color =
        if (isDarkTheme) Color(183, 116, 201, 255)
        else Color(73, 4, 119, 255)

    fun pickedSongColor(): Color =
        if (isDarkTheme) Color(143, 159, 253, 255)
        else Color(0, 10, 51, 255)

    fun disabledThemedColor(): Color =
        if (isDarkTheme) Color(38, 38, 38, 204)
        else Color(98, 98, 98, 204)

    fun unSelectedThumbColor(): Color =
        if (isDarkTheme) Color(153, 153, 178, 204)
        else Color(32, 39, 40, 204)

    fun selectedThumbColor(): Color =
        if (isDarkTheme) Color(54, 54, 159, 204)
        else Color(16, 47, 49, 204)

    fun dialogBackGroundColor(): Color =
        if (isDarkTheme) Color(13, 16, 13, 255)
        else Color(179, 252, 201, 255)

    fun getColorScheme(): ColorScheme = cS

    fun floatingButtonColor(): Color =
        if (isDarkTheme) Color(72, 41, 41, 255) else Color(112, 76, 76, 255)

}