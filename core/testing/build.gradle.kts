plugins {
    alias(libs.plugins.subzero.android.library)
}

android {
    namespace = "com.subzero.core.testing"
}

dependencies {
    api(libs.junit4)
    api(libs.truth)
    api(libs.turbine)
    api(libs.kotlinx.coroutines.test)
    api(libs.androidx.test.ext.junit)

    implementation(projects.core.common)
    implementation(projects.core.domain)
}
