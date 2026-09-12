package com.subzero.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Shared Android + Kotlin configuration for every Android module.
 *
 * AGP 9 ships built-in Kotlin support, so the `org.jetbrains.kotlin.android` plugin is
 * intentionally NOT applied; compiler options are set through [KotlinAndroidProjectExtension].
 */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = libs.version("compileSdk")

        defaultConfig.minSdk = libs.version("minSdk")

        compileOptions.apply {
            // Java 17 language features are desugared by D8 for all minSdk levels.
            // Library APIs (java.time etc.) are native from minSdk 26, so no core-library desugaring.
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        lint.apply {
            // Lint problems are build failures. Suppress individually, with a reason, never globally.
            warningsAsErrors = true
            abortOnError = true
        }
    }

    configureKotlin<KotlinAndroidProjectExtension>()
}

/** Shared Kotlin configuration for pure-JVM modules (e.g. `core:domain`). */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    configureKotlin<KotlinJvmProjectExtension>()
}

private inline fun <reified T : KotlinBaseExtension> Project.configureKotlin() = configure<T> {
    // Compiler warnings fail the build by default. Override locally with -PwarningsAsErrors=false.
    val warningsAsErrors = providers.gradleProperty("warningsAsErrors")
        .map(String::toBoolean)
        .orElse(true)

    when (this) {
        is KotlinAndroidProjectExtension -> compilerOptions
        is KotlinJvmProjectExtension -> compilerOptions
        else -> error("Unsupported Kotlin extension: ${T::class}")
    }.apply {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(warningsAsErrors)
        freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
}
