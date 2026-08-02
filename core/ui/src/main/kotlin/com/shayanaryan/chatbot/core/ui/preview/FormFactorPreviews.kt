package com.shayanaryan.chatbot.core.ui.preview

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
