import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    // JVM desktop
    jvm()

    // iOS targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "shared"
            isStatic = false
            freeCompilerArgs += "-Xbinary=bundleId=com.llamatik.shared"
            freeCompilerArgs += "-Xbinary=ios_version_min=16.6"
            freeCompilerArgs += "-Xoverride-konan-properties=osVersionMin.ios=16.6"
        }
    }

    sourceSets {
        all {
            languageSettings {
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
                optIn("androidx.compose.foundation.layout.ExperimentalLayoutApi")
                optIn("androidx.compose.material3.ExperimentalMaterial3Api")
            }
        }

        commonMain.dependencies {
            api(project(":library"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.animation)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.io)

            implementation(libs.ktor.client)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.cio)
            implementation(libs.ktor.server.content.negotiation)
            implementation(libs.ktor.server.serialization.kotlinx.json)

            // Kamel for image loading
            implementation(libs.kamel)
            implementation(libs.kamel.default)

            // Voyager for Navigation
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.bottom.sheet.navigator)
            implementation(libs.voyager.tab.navigator)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.koin)

            // Multiplatform Settings
            implementation(libs.multiplatform.settings.no.arg)
            implementation(libs.multiplatform.settings.serialization)

            // Dependency Injection
            implementation(libs.koin.core)

            // Logging
            implementation(libs.kermit)

            implementation(libs.junit)

            implementation(libs.xmlutil.core)
            implementation(libs.xmlutil.serialization)

            implementation(libs.richeditor.compose)
            implementation(libs.koalaplot.core)

            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.urlencoder)
        }

        androidMain.dependencies {
            implementation(libs.androidx.compose.runtime)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.work.routine.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.activity.ktx)
            implementation(libs.ktor.client.android)
            implementation(libs.bouquet)
            implementation(libs.okhttp)

            implementation(libs.android.play.review)
            implementation(libs.android.play.review.ktx)
            implementation(libs.kotlinx.coroutines.play.services)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.stately.common)
            implementation(compose.components.resources)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.common)
        }

        commonTest.dependencies {
            implementation(libs.junit)
            implementation(libs.koin.test)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.multiplatform.settings.test)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.llamatik.app.resources"
    generateResClass = always
}

android {
    namespace = "com.llamatik.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
