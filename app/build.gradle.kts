import java.util.Properties

plugins {
    alias(libs.plugins.subzero.android.application)
    alias(libs.plugins.subzero.android.application.compose)
    alias(libs.plugins.subzero.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing comes from an untracked keystore.properties (see keystore.properties.example)
// or from CI environment variables. Without either, release builds stay unsigned.
val keystoreProperties: Properties? = rootProject.file("keystore.properties")
    .takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use(::load) } }
    ?: System.getenv("SUBZERO_KEYSTORE_PATH")?.let { path ->
        Properties().apply {
            setProperty("storeFile", path)
            setProperty("storePassword", System.getenv("SUBZERO_KEYSTORE_PASSWORD").orEmpty())
            setProperty("keyAlias", System.getenv("SUBZERO_KEY_ALIAS").orEmpty())
            setProperty("keyPassword", System.getenv("SUBZERO_KEY_PASSWORD").orEmpty())
        }
    }

android {
    // The namespace is the code package and stays com.subzero.app; the applicationId is the
    // Play Store identity, which is permanent once uploaded and must be one nobody else holds.
    namespace = "com.subzero.app"

    defaultConfig {
        applicationId = "com.yazanprogrammer.subzero"
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (keystoreProperties != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = if (keystoreProperties != null) signingConfigs.getByName("release") else null
        }
    }

    buildFeatures {
        // Only for BuildConfig.VERSION_NAME shown on the About screen.
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.navigation)
    implementation(projects.core.notifications)
    implementation(projects.core.ai)

    implementation(projects.feature.onboarding)
    implementation(projects.feature.home)
    implementation(projects.feature.subscriptions)
    implementation(projects.feature.calendar)
    implementation(projects.feature.insights)
    implementation(projects.feature.settings)
    implementation(projects.feature.assistant)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.core.testing)
    testImplementation(testFixtures(projects.core.domain))
    androidTestImplementation(projects.core.testing)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
