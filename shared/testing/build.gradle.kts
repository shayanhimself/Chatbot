plugins {
    alias(libs.plugins.chatbot.kmp.library)
}

kotlin {
    android {
        namespace = "com.shayanaryan.chatbot.shared.testing"

        withHostTestBuilder {}.configure {
            isReturnDefaultValues = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // api, not implementation: every fake's public surface is a :shared type: the
            // repository interface it implements, the models it returns. A consumer that
            // sees the fake must see those too.
            api(projects.shared)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
