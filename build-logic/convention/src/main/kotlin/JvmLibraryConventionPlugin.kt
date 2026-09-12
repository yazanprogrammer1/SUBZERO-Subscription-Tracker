import com.subzero.buildlogic.configureKotlinJvm
import com.subzero.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/** Pure Kotlin/JVM module: no Android dependency, fast unit tests. Used for `core:domain`. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.jvm")
            apply(plugin = "com.android.lint")

            configureKotlinJvm()

            dependencies {
                "testImplementation"(libs.findLibrary("junit4").get())
                "testImplementation"(libs.findLibrary("truth").get())
            }
        }
    }
}
