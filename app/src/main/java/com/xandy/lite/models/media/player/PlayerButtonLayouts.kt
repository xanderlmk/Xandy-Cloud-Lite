package com.xandy.lite.models.media.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import com.xandy.lite.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import androidx.media3.session.R as AndroidR

private const val PLAYER_CONTROLS = "PlayerControls"
private const val XC_COMMAND_BUTTON = "XCCommandButton"


object ButtonType {
    const val BUTTON_LAYOUT = "button_layout"
    const val COMMAND_REPEAT = "Cycle_Repeat"
    const val COMMAND_SHUFFLE = "Shuffle_Songs"
    const val COMMAND_FAST_FORWARD = "Fast_Forward"
    const val COMMAND_FAVORITE = "Favorite"
    const val COMMAND_REWIND = "Rewind_Xandy"
    const val COMMAND_CHANGE_BUTTON = "Change_Button"
    const val COMMAND_CHANGE_LAYOUT = "Change_Button_Layout"
}


@Serializable
@SerialName(PLAYER_CONTROLS)
sealed class PlayerControls {
    @Serializable
    @SerialName("$PLAYER_CONTROLS.DEFAULT")
    data object Default : PlayerControls()

    @Serializable
    @SerialName("$PLAYER_CONTROLS.REVERSED")
    data object Reversed : PlayerControls()

    @Serializable
    @SerialName("$PLAYER_CONTROLS.WITH_SETTINGS")
    data class Configurable(
        val button: XCCommandButton = XCCommandButton.Repeat,
        val included: List<XCCommandButton> =
            listOf(
                XCCommandButton.Repeat, XCCommandButton.Shuffle,
                XCCommandButton.Favorite, XCCommandButton.FastForward,
                XCCommandButton.Rewind
            )
    ) : PlayerControls() {
        private val referenceOrder = sortedMapOf(
            0 to XCCommandButton.Repeat,
            1 to XCCommandButton.Shuffle,
            2 to XCCommandButton.Favorite,
            3 to XCCommandButton.FastForward,
            4 to XCCommandButton.Rewind
        ).values.toList()

        fun switchButton(): Configurable {
            val nextIndex = (this.included.indexOf(this.button) + 1) % this.included.size
            return Configurable(included[nextIndex], included)
        }

        fun toggleButton(button: XCCommandButton) =
            if (this.included.contains(button)) removeButton(button)
            else addButton(button)

        fun canRemove(button: XCCommandButton) =
            if (this.included.contains(button)) canRemoveButton() else true


        private fun addButton(button: XCCommandButton): Configurable {

            val refOrder = referenceOrder
            val buttonRefIdx = refOrder.indexOf(button)

            // if no canonical position known, append
            if (buttonRefIdx == -1) {
                val newList = included.toMutableList()
                newList.add(button)
                return Configurable(this.button, newList)
            }

            // find the first existing item whose ref index is > buttonRefIdx
            val newList = included.toMutableList()
            var insertAt = newList.size
            for ((i, existing) in newList.withIndex()) {
                val existingRefIdx = refOrder.indexOf(existing)
                if (existingRefIdx > buttonRefIdx) {
                    insertAt = i
                    break
                }
            }
            newList.add(insertAt, button)
            return Configurable(this.button, newList)
        }

        /** If there is less than 2 buttons available, the user cannot remove the button */
        private fun cannotRemoveButton() = (this.included.size - 1) < 2
        private fun canRemoveButton() = (this.included.size - 1) >= 2
        private fun removeButton(button: XCCommandButton): Configurable {
            if (this.cannotRemoveButton()) return this
            val newList = included.filter { it != button }
            // ensure the current selected button is valid
            val newSelected = if (this.button == button) {
                newList.firstOrNull() ?: XCCommandButton.Repeat
            } else this.button
            return Configurable(newSelected, newList)
        }
    }

    @Serializable
    @SerialName("$PLAYER_CONTROLS.Custom")
    @OptIn(UnstableApi::class)
    data class Custom(
        val buttonOne: CustomCB? = null, val prevButton: CustomCB = CustomCB.Previous,
        val nextButton: CustomCB = CustomCB.Next, val buttonTwo: CustomCB? = null,
        val customConfig: CustomConfigButton = CustomConfigButton()
    ) : PlayerControls() {
        companion object {
            const val OVERFLOW_ONE = 0
            const val PREV = 1
            const val NEXT = 2
            const val OVERFLOW_TWO = 3
            const val UNSET = -1
        }

        fun toListOfNotNull() = listOfNotNull(
            buttonOne?.let { Pair(it, CommandButton.SLOT_OVERFLOW) },
            Pair(prevButton, CommandButton.SLOT_BACK),
            Pair(nextButton, CommandButton.SLOT_FORWARD),
            buttonTwo?.let { Pair(it, CommandButton.SLOT_OVERFLOW) }
        )

        fun toList() = listOf(buttonOne, prevButton, nextButton, buttonTwo)

        private fun notInOverflow(int: Int) = int != OVERFLOW_ONE && int != OVERFLOW_TWO
        fun isInOverflow(int: Int) = int == OVERFLOW_ONE || int == OVERFLOW_TWO

        fun toNull(location: Int): Custom {
            if (notInOverflow(location)) return this
            val list = mutableListOf(buttonOne, prevButton, nextButton, buttonTwo)
            val new = list[location]?.takeIf { it != CustomCB.Config }?.toConfigCB()
            list[location] = null
            val newButtonLocation =
                getNewButtonLocation(list, location, list.contains(CustomCB.Config))
            val newConfigList = this.customConfig.included +
                    (new?.let { listOf(it) } ?: emptyList())
            return this.copy(
                buttonOne = list[OVERFLOW_ONE],
                prevButton = prevButton,
                nextButton = nextButton,
                buttonTwo = list[OVERFLOW_TWO],
                customConfig = this.customConfig.copy(
                    included = newConfigList, location = newButtonLocation
                )
            )
        }

        fun canChange(current: CustomCB, location: Int): Boolean {
            val list = mutableListOf(buttonOne, prevButton, nextButton, buttonTwo)
            val existingIndex = list.indexOf(current)
            if (existingIndex == location) return true

            val temp = list[location]

            return !(existingIndex >= 0 && existingIndex in PREV..NEXT && temp == null)
        }

        fun canChange(pickedIdx: Int, location: Int): Boolean {
            val list = mutableListOf(buttonOne, prevButton, nextButton, buttonTwo)
            return if (pickedIdx == -1) true
            else if (notInOverflow(pickedIdx) && list[location] == null) false
            else true
        }

        fun notConfigChangeable(
            list: List<CustomCB?>, location: Int, currentIdx: Int
        ) = list.contains(CustomCB.Config) &&
                currentIdx == this.customConfig.location &&
                location == list.indexOf(CustomCB.Config)

        fun configChangeable(
            list: List<CustomCB?>, location: Int, currentIdx: Int
        ) = !notConfigChangeable(list, location, currentIdx)

        fun onChange(new: CustomCB, location: Int): Pair<Custom, Boolean> {
            if (location !in OVERFLOW_ONE..OVERFLOW_TWO) return Pair(this, false)

            val list = mutableListOf(buttonOne, prevButton, nextButton, buttonTwo)
            val existingIndex = list.indexOf(new)
            // nothing to do if new already in the requested location
            if (existingIndex == location) return Pair(this, true)

            val temp = list[location]
            var newLocation: Int? = null
            if (existingIndex >= 0) {
                // If swapping would set a mandatory slot (1..3) to null, don't allow it.
                // This preserves the invariant that prev/pause/next cannot be null.
                if ((existingIndex in PREV..NEXT && temp == null) ||
                    notConfigChangeable(list, location, existingIndex)
                ) {
                    return Pair(this, false)
                }
                if (this.hasConfigButton() && location == this.customConfig.location)
                    newLocation = existingIndex
                // perform swap
                list[location] = new
                list[existingIndex] = temp
            } else {
                // new not present — place it at the requested location
                list[location] = new
            }
            val customConfig = this.customConfig
            val buttonLocation =
                newLocation ?: getNewButtonLocation(list, location, new == CustomCB.Config)
            val newConfigList = customConfig.included.filter { it.toCustomCB() !in list }
            return Pair(
                this.copy(
                    buttonOne = list[OVERFLOW_ONE],
                    prevButton = list[PREV]!!,   // guaranteed non-null from data invariants
                    nextButton = list[NEXT]!!,
                    buttonTwo = list[OVERFLOW_TWO],
                    this.customConfig.copy(location = buttonLocation, included = newConfigList)
                ), true
            )
        }

        fun isSelectedConfigIdx(idx: Int) =
            this.toListOfNotNull().map { it.first }.contains(CustomCB.Config) &&
                    this.customConfig.location == idx

        fun hasConfigButton() = this.toListOfNotNull().map { it.first }.contains(CustomCB.Config)
        fun switchButton(): Custom {
            val new = this.customConfig.switchButton()
            val pc = onChange(new.button.toCustomCB(), new.location)
            return pc.first.copy(customConfig = new)
        }

        fun toggleButton(button: ConfigCB) =
            this.copy(customConfig = this.customConfig.toggleButton(button))

        fun canRemove(button: ConfigCB) = this.customConfig.canRemove(button)

        private fun List<CustomCB?>.isValid(idx: Int) =
            this[idx] != null && this[idx] != CustomCB.Config

        private fun getNewButtonLocation(
            list: List<CustomCB?>, location: Int, expression: Boolean
        ) = if (expression) {
            when (location) {
                OVERFLOW_ONE -> if (list.isValid(OVERFLOW_TWO)) OVERFLOW_TWO else PREV
                PREV -> when {
                    list.isValid(OVERFLOW_TWO) -> OVERFLOW_TWO
                    list.isValid(OVERFLOW_ONE) -> OVERFLOW_ONE
                    else -> NEXT
                }

                NEXT -> when {
                    list.isValid(OVERFLOW_TWO) -> OVERFLOW_TWO
                    list.isValid(OVERFLOW_ONE) -> OVERFLOW_ONE
                    else -> PREV
                }

                OVERFLOW_TWO -> if (list.isValid(OVERFLOW_ONE)) OVERFLOW_ONE else PREV
                else -> error("Invalid location")
            }
        } else this.customConfig.location

    }


    /** Returns if [PlayerControls] is Configurable or Custom */
    fun isCustomBased() = this is Configurable || this is Custom
}

@Serializable
@SerialName(XC_COMMAND_BUTTON)
data class CustomConfigButton(
    val button: ConfigCB = ConfigCB.Repeat,
    val included: List<ConfigCB> = ConfigCB.entries.filter { it.isNotDefault() },
    val location: Int = PlayerControls.Custom.UNSET
) {
    private val referenceOrder = sortedMapOf(
        0 to ConfigCB.Repeat,
        1 to ConfigCB.Shuffle,
        2 to ConfigCB.Favorite,
        3 to ConfigCB.FastForward,
        4 to ConfigCB.Rewind,
        5 to ConfigCB.Next,
        6 to ConfigCB.Previous
    ).values.toList()

    internal fun switchButton(): CustomConfigButton {
        val nextIndex = (this.included.indexOf(this.button) + 1) % this.included.size
        return CustomConfigButton(included[nextIndex], included, this.location)
    }

    internal fun toggleButton(button: ConfigCB) =
        if (this.included.contains(button)) removeButton(button)
        else addButton(button)

    private fun addButton(button: ConfigCB): CustomConfigButton {
        val refOrder = referenceOrder
        val buttonRefIdx = refOrder.indexOf(button)

        // if no canonical position known, append
        if (buttonRefIdx == -1) {
            val newList = included.toMutableList()
            newList.add(button)
            return CustomConfigButton(this.button, newList, this.location)
        }

        // find the first existing item whose ref index is > buttonRefIdx
        val newList = included.toMutableList()
        var insertAt = newList.size
        for ((i, existing) in newList.withIndex()) {
            val existingRefIdx = refOrder.indexOf(existing)
            if (existingRefIdx > buttonRefIdx) {
                insertAt = i
                break
            }
        }
        newList.add(insertAt, button)
        return CustomConfigButton(this.button, newList, this.location)
    }

    internal fun canRemove(button: ConfigCB) =
        if (this.included.contains(button)) canRemoveButton() else true

    private fun cannotRemoveButton() = (this.included.size - 1) < 2
    private fun canRemoveButton() = (this.included.size - 1) >= 2
    private fun removeButton(button: ConfigCB): CustomConfigButton {
        if (this.cannotRemoveButton()) return this
        val newList = included.filter { it != button }
        // ensure the current selected button is valid
        val newSelected = if (this.button == button) {
            newList.firstOrNull() ?: ConfigCB.Repeat
        } else this.button
        return CustomConfigButton(newSelected, newList, this.location)
    }

}

@Serializable
@SerialName(XC_COMMAND_BUTTON)
enum class XCCommandButton(val icon: Int) {
    FastForward(R.drawable.fast_forward),
    Rewind(R.drawable.fast_rewind),
    Repeat(R.drawable.sharp_repeat),
    Shuffle(AndroidR.drawable.media3_icon_shuffle_on),
    Favorite(AndroidR.drawable.media3_icon_heart_filled)
}

@Serializable
@SerialName(XC_COMMAND_BUTTON)
enum class CustomCB(val icon: Int) {
    FastForward(R.drawable.fast_forward),
    Rewind(R.drawable.fast_rewind),
    Repeat(R.drawable.sharp_repeat),
    Shuffle(AndroidR.drawable.media3_icon_shuffle_on),

    Config(R.drawable.outline_change_circle),
    Favorite(AndroidR.drawable.media3_icon_heart_filled),
    Next(AndroidR.drawable.media3_icon_next),
    Previous(AndroidR.drawable.media3_icon_previous),
}

@Serializable
@SerialName("$XC_COMMAND_BUTTON.Config")
enum class ConfigCB(val icon: Int) {
    Repeat(R.drawable.sharp_repeat),
    Shuffle(AndroidR.drawable.media3_icon_shuffle_on),
    Favorite(AndroidR.drawable.media3_icon_heart_filled),
    FastForward(R.drawable.fast_forward),
    Rewind(R.drawable.fast_rewind),
    Next(AndroidR.drawable.media3_icon_next),
    Previous(AndroidR.drawable.media3_icon_previous);

    /** Returns true if it is NOT Next and NOT Previous */
    internal fun isNotDefault() = this != Next && this != Previous
}

private fun CustomCB.toConfigCB(): ConfigCB = when (this) {
    CustomCB.FastForward -> ConfigCB.FastForward
    CustomCB.Rewind -> ConfigCB.Rewind
    CustomCB.Repeat -> ConfigCB.Repeat
    CustomCB.Shuffle -> ConfigCB.Shuffle
    CustomCB.Favorite -> ConfigCB.Favorite
    CustomCB.Next -> ConfigCB.Next
    CustomCB.Previous -> ConfigCB.Previous
    CustomCB.Config -> error("This should now happen and is invalid")
}

fun ConfigCB.toCustomCB() = when (this) {
    ConfigCB.Repeat -> CustomCB.Repeat
    ConfigCB.Shuffle -> CustomCB.Shuffle
    ConfigCB.Favorite -> CustomCB.Favorite
    ConfigCB.FastForward -> CustomCB.FastForward
    ConfigCB.Rewind -> CustomCB.Rewind
    ConfigCB.Next -> CustomCB.Next
    ConfigCB.Previous -> CustomCB.Previous
}

