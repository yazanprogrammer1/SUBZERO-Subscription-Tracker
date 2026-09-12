// Navigation contracts shared by every feature. Features navigate by pushing these keys;
// the app module maps keys to screens. No feature depends on another feature.
plugins {
    alias(libs.plugins.subzero.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.subzero.core.navigation"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    api(libs.androidx.navigation3.runtime)
    api(libs.kotlinx.serialization.json)
}
