plugins {
    alias(libs.plugins.subzero.android.library)
    alias(libs.plugins.subzero.android.library.compose)
}

android {
    namespace = "com.subzero.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.core)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.animation)
    api(libs.androidx.compose.ui.tooling.preview)
    api(projects.core.domain)
    implementation(libs.androidx.core.ktx)
}
