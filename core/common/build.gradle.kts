plugins {
    alias(libs.plugins.subzero.android.library)
    alias(libs.plugins.subzero.hilt)
}

android {
    namespace = "com.subzero.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}
