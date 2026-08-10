plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        // Silences the Beta warning on the database constructor's expect/actual pair.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.shayanaryan.chatbot.shared"
        compileSdk = 37
        minSdk = 31

        withHostTestBuilder {}.configure {
            // enables commonTest on JVM; returnDefaultValues stops android.jar stubs
            // (e.g. android.util.Log, touched by OkHttp's platform detection) from
            // throwing "not mocked" on the host test classpath.
            isReturnDefaultValues = true
            // Packages the merged manifest, resources and assets for host tests and points
            // Robolectric at them, which is also what puts robolectric.properties on its path.
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            api(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            api(libs.androidx.datastore.preferences)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.tink.android)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
        // No typed accessor exists for this source set.
        getByName("androidHostTest").dependencies {
            implementation(projects.shared.testing)
            implementation(libs.junit)
            implementation(libs.robolectric)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.ext.junit)
        }
    }
}

dependencies {
    // KSP configurations are created per target after the kotlin {} block evaluates, so there is
    // no typed accessor for them and the catch-all `ksp(…)` is deprecated in KSP 2.
    // add("ksp<Target>", …) is the only available spelling.
    add("kspAndroid", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

// Robolectric needs a Java 21 runtime for SDK 36; compile toolchain stays 17.
tasks.withType<Test>().configureEach {
    javaLauncher.set(
        project.extensions.getByType<JavaToolchainService>().launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        },
    )
}
