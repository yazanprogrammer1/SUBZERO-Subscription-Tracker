plugins {
    alias(libs.plugins.subzero.android.feature)
}

android {
    namespace = "com.subzero.feature.settings"
}

dependencies {
    implementation(projects.core.notifications)
    implementation(libs.androidx.activity.compose)
}
