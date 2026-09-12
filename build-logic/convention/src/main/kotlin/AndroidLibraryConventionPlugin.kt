import com.android.build.api.dsl.LibraryExtension
import com.subzero.buildlogic.configureKotlinAndroid
import com.subzero.buildlogic.libs
import com.subzero.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                testOptions.targetSdk = libs.version("targetSdk")
                lint.targetSdk = libs.version("targetSdk")
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                testOptions.animationsDisabled = true
                // Resources in ":core:designsystem" must be prefixed "core_designsystem_",
                // which prevents silent resource collisions between modules.
                resourcePrefix = path.split("""\W""".toRegex())
                    .drop(1)
                    .distinct()
                    .joinToString(separator = "_")
                    .lowercase() + "_"
            }

            // Robolectric reaches into java.io internals that JDK 17+ seals by default.
            tasks.withType<Test>().configureEach {
                jvmArgs(
                    "--add-opens", "java.base/java.io=ALL-UNNAMED",
                    "--add-exports", "java.base/jdk.internal.access=ALL-UNNAMED",
                )
            }

            dependencies {
                "testImplementation"(libs.findLibrary("junit4").get())
                "testImplementation"(libs.findLibrary("truth").get())
                "androidTestImplementation"(libs.findLibrary("androidx-test-ext-junit").get())
                "androidTestImplementation"(libs.findLibrary("truth").get())
            }
        }
    }
}
