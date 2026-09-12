import com.android.build.api.dsl.LibraryExtension
import com.subzero.buildlogic.configureKotlinAndroid
import com.subzero.buildlogic.libs
import com.subzero.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

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

            dependencies {
                "testImplementation"(libs.findLibrary("junit4").get())
                "testImplementation"(libs.findLibrary("truth").get())
                "androidTestImplementation"(libs.findLibrary("androidx-test-ext-junit").get())
                "androidTestImplementation"(libs.findLibrary("truth").get())
            }
        }
    }
}
