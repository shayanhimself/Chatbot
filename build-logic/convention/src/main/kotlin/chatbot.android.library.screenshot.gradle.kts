import com.android.compose.screenshot.gradle.ScreenshotTestOptions

plugins {
    id("com.android.library")
    id("chatbot.android.library.compose")
    id("chatbot.robolectric")
}

android {
    // Gates screenshotTest source-set creation
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Applied here rather than in the plugins block: it refuses to apply until the gate above is set,
// and everything in a plugins block runs before the body.
apply(plugin = "com.android.compose.screenshot")

// The screenshot plugin leaves `check` scheduling only the render task, so a golden can drift
// without any gate noticing. `dependsOn` by name resolves when the task graph is built, which is
// after the plugin applied above has registered it.
tasks.named("check") {
    dependsOn("validateDebugScreenshotTest")
}

// Lets a small share of screenshot pixels differ before validation fails. The default of zero demands
// identical pixels, which a golden recorded on macOS never is on the Linux runner. Set after the
// apply above, which registers this extension.
extensions.configure<ScreenshotTestOptions> {
    imageDifferenceThreshold = SCREENSHOT_IMAGE_DIFFERENCE_THRESHOLD
}
