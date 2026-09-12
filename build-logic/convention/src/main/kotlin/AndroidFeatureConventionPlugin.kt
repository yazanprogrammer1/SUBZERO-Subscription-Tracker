import com.android.build.api.dsl.LibraryExtension
import com.subzero.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * A feature module: Compose UI + ViewModels for one product area.
 * Feature modules depend on core modules only, never on each other.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "subzero.android.library")
            apply(plugin = "subzero.android.library.compose")
            apply(plugin = "subzero.hilt")

            // Compose UI tests run on the JVM through Robolectric so they are part of `gradlew test`.
            extensions.configure<LibraryExtension> {
                testOptions.unitTests.isIncludeAndroidResources = true
            }

            dependencies {
                "implementation"(project(":core:common"))
                "implementation"(project(":core:domain"))
                "implementation"(project(":core:designsystem"))
                "implementation"(project(":core:navigation"))

                "implementation"(libs.findLibrary("androidx-hilt-lifecycle-viewmodel-compose").get())
                "implementation"(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                "implementation"(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                "implementation"(libs.findLibrary("androidx-navigation3-runtime").get())
                "implementation"(libs.findLibrary("kotlinx-coroutines-core").get())

                "testImplementation"(project(":core:testing"))
                "testImplementation"(testFixtures(project(":core:domain")))
                "testImplementation"(libs.findLibrary("kotlinx-coroutines-test").get())
                "testImplementation"(libs.findLibrary("turbine").get())
                "testImplementation"(libs.findLibrary("robolectric").get())
                "testImplementation"(libs.findLibrary("androidx-test-core").get())
                "testImplementation"(libs.findLibrary("androidx-compose-ui-test-junit4").get())
                "testImplementation"(libs.findLibrary("androidx-compose-ui-test-manifest").get())
                "androidTestImplementation"(project(":core:testing"))
                "androidTestImplementation"(testFixtures(project(":core:domain")))
                "androidTestImplementation"(libs.findLibrary("androidx-compose-ui-test-junit4").get())
                "debugImplementation"(libs.findLibrary("androidx-compose-ui-test-manifest").get())
            }
        }
    }
}
