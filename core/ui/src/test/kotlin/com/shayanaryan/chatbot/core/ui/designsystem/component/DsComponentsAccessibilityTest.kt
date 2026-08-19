package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.core.testing.string
import com.shayanaryan.chatbot.core.ui.R
import com.shayanaryan.chatbot.core.ui.designsystem.icon.DsIcon
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val BUTTON_LABEL = "Continue"
private const val ICON_BUTTON_LABEL = "Close the sheet"
private const val CHIP_LABEL = "Sonnet"
private const val ICON_LABEL = "Offline"

private val touchTargetTolerance = 0.5.dp


@RunWith(AndroidJUnit4::class)
class DsComponentsAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `a button is at least the minimum tappable size`() {
        composeRule.setContent {
            ChatbotTheme {
                DsButton(text = BUTTON_LABEL, onClick = {})
            }
        }

        composeRule
            .onNodeWithText(BUTTON_LABEL)
            .assertTouchTargetMeetsMinimum(composeRule.density)
    }

    @Test
    fun `an icon button is at least the minimum tappable size`() {
        composeRule.setContent {
            ChatbotTheme {
                DsIconButton(
                    glyph = Glyphs.CLOSE,
                    contentDescription = ICON_BUTTON_LABEL,
                    onClick = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription(ICON_BUTTON_LABEL)
            .assertTouchTargetMeetsMinimum(composeRule.density)
    }

    @Test
    fun `a chip's dismiss affordance is at least the minimum tappable size`() {
        composeRule.setContent {
            ChatbotTheme {
                DsChip(
                    label = CHIP_LABEL,
                    onClick = {},
                    variant = ChipVariant.Input,
                    onDismiss = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription(string(R.string.core_ui_dismiss, CHIP_LABEL))
            .assertTouchTargetMeetsMinimum(composeRule.density)
    }

    @Test
    fun `a loading button announces the wait`() {
        composeRule.setContent {
            ChatbotTheme {
                DsButton(text = BUTTON_LABEL, onClick = {}, loading = true)
            }
        }

        val node = composeRule.onNodeWithText(BUTTON_LABEL).fetchSemanticsNode()

        assertEquals(
            string(R.string.core_ui_loading),
            node.config[SemanticsProperties.StateDescription],
        )
    }

    @Test
    fun `a resting button announces no state`() {
        composeRule.setContent {
            ChatbotTheme {
                DsButton(text = BUTTON_LABEL, onClick = {})
            }
        }

        val node = composeRule.onNodeWithText(BUTTON_LABEL).fetchSemanticsNode()

        assertEquals(null, node.config.getOrNull(SemanticsProperties.StateDescription))
    }

    @Test
    fun `a decorative icon is absent from the accessibility tree`() {
        composeRule.setContent {
            ChatbotTheme {
                DsIcon(glyph = Glyphs.CLOUD_OFF, contentDescription = null)
            }
        }

        composeRule.onNodeWithText(Glyphs.CLOUD_OFF).assertDoesNotExist()
    }

    @Test
    fun `a labelled icon carries its label and not its ligature`() {
        composeRule.setContent {
            ChatbotTheme {
                DsIcon(glyph = Glyphs.CLOUD_OFF, contentDescription = ICON_LABEL)
            }
        }

        composeRule.onNodeWithContentDescription(ICON_LABEL).assertExists()
        composeRule.onNodeWithText(Glyphs.CLOUD_OFF).assertDoesNotExist()
    }
}


/**
 * Asserts the node's touch target is at least the design system's minimum on both axes.
 *
 * Compose ships an "at least" assertion for layout bounds and an exact one for touch bounds, but
 * the contract is a minimum on the touch bounds: a component is free to exceed it, and one that
 * expands only its touch target leaves its layout bounds smaller than what it accepts a tap in.
 */
private fun SemanticsNodeInteraction.assertTouchTargetMeetsMinimum(density: Density) {
    val touchBounds = fetchSemanticsNode().touchBoundsInRoot
    with(density) {
        val height = touchBounds.height.toDp()
        val width = touchBounds.width.toDp()
        assertTrue(
            "touch height $height is below ${Spacing.touchTargetMin}",
            height + touchTargetTolerance >= Spacing.touchTargetMin,
        )
        assertTrue(
            "touch width $width is below ${Spacing.touchTargetMin}",
            width + touchTargetTolerance >= Spacing.touchTargetMin,
        )
    }
}
