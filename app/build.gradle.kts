plugins {
    alias(libs.plugins.chatbot.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/**
 * The developer's own key, from local.properties. Will be removed when onboarding is implemented.
 */
val devApiKey: Provider<String> =
    providers
        .environmentVariable("ANTHROPIC_API_KEY")
        .orElse(
            providers
                .fileContents(layout.projectDirectory.file("../local.properties"))
                .asText
                .map { contents ->
                    contents
                        .lineSequence()
                        .map(String::trim)
                        .firstOrNull { it.startsWith("anthropic.api.key=") }
                        ?.substringAfter('=')
                        ?.trim()
                        .orEmpty()
                },
        ).orElse("")

android {
    namespace = "com.shayanaryan.chatbot"
    defaultConfig {
        applicationId = "com.shayanaryan.chatbot"
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "com.shayanaryan.chatbot.HiltTestRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "DEV_API_KEY", "\"${devApiKey.get()}\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    buildFeatures {
        aidl = false
        buildConfig = true
        shaders = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(projects.shared)
    implementation(projects.core.ui)
    implementation(projects.feature.conversation)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.settings)
    implementation(projects.feature.reminders)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.adaptive.navigation3)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(projects.shared.testing)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
