import androidx.room.gradle.RoomExtension
import com.subzero.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "androidx.room")
            apply(plugin = "com.google.devtools.ksp")

            extensions.configure<RoomExtension> {
                // One schema JSON per database version, committed to git. This is what makes
                // migrations testable (MigrationTestHelper) and is non-negotiable for production.
                schemaDirectory("$projectDir/schemas")
            }

            dependencies {
                "implementation"(libs.findLibrary("androidx-room-runtime").get())
                "implementation"(libs.findLibrary("androidx-room-ktx").get())
                "ksp"(libs.findLibrary("androidx-room-compiler").get())
                "androidTestImplementation"(libs.findLibrary("androidx-room-testing").get())
            }
        }
    }
}
