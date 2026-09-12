import com.android.build.api.dsl.ApplicationExtension
import com.subzero.buildlogic.configureKotlinAndroid
import com.subzero.buildlogic.libs
import com.subzero.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = libs.version("targetSdk")
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                testOptions.animationsDisabled = true
                // Lint the whole dependency graph from the app module.
                lint.checkDependencies = true
            }
        }
    }
}
