package com.shayanaryan.chatbot.core.testing.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

/**
 * The four widths the app is expected to survive. A list-detail layout changes shape between them,
 * so a golden per form factor is what catches a pane that stops laying out.
 */
@Preview(name = "phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "foldable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "tablet", device = Devices.TABLET, showBackground = true)
@Preview(name = "desktop", device = Devices.DESKTOP, showBackground = true)
annotation class FormFactorPreviews

/**
 * The text sizes the app is expected to survive. Every text style is sp-based, so a layout that
 * fits at the default scale can still clip at the largest accessibility setting, and a golden per
 * scale is what catches it.
 */
@Preview(name = "font-1x", fontScale = 1.0f, showBackground = true)
@Preview(name = "font-1-5x", fontScale = 1.5f, showBackground = true)
@Preview(name = "font-2x", fontScale = 2.0f, showBackground = true)
annotation class FontScalePreviews

/**
 * Both color schemes, at a phone's size. The theme reads the system setting, so uiMode is what
 * picks the scheme, and a golden per mode is what catches a color that only resolves in one of
 * them.
 */
@Preview(
    name = "dark",
    device = Devices.PHONE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Preview(
    name = "light",
    device = Devices.PHONE,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
annotation class ThemePreviews
