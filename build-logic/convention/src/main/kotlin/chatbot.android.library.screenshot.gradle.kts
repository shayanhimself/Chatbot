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
