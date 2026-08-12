plugins {
    alias(libs.plugins.chatbot.kmp.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

kotlin {
    compilerOptions {
        // Silences the Beta warning on the database constructor's expect/actual pair.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.shayanaryan.chatbot.shared"

        withHostTestBuilder {}.configure {
            // enables commonTest on JVM; returnDefaultValues stops android.jar stubs
            // (e.g. android.util.Log, touched by OkHttp's platform detection) from
            // throwing "not mocked" on the host test classpath.
            isReturnDefaultValues = true
            // Packages the merged manifest, resources and assets for host tests and points
            // Robolectric at them, which is also what puts robolectric.properties on its path.
            isIncludeAndroidResources = true
        }

        // Device tests are off by default under the Android-KMP plugin; this is what creates the
        // `androidDeviceTest` source set and the tasks that build and run its APK.
        withDeviceTestBuilder {
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            // Points back at a module that depends on this one, which only a test compilation may
            // do: no main compilation here sees it, so the task graph stays acyclic. It is
            // declared once, since androidHostTest inherits what commonTest resolves.
            implementation(projects.shared.testing)
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.robolectric)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.ext.junit)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.junit)
            implementation(libs.androidx.test.runner)
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
