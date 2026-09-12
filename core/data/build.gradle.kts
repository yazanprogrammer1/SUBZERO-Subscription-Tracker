plugins {
    alias(libs.plugins.subzero.android.library)
    alias(libs.plugins.subzero.android.room)
    alias(libs.plugins.subzero.hilt)
}

android {
    namespace = "com.subzero.core.data"
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.common)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(projects.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(projects.core.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.runner)
}
