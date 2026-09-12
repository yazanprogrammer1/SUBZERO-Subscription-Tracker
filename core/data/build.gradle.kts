plugins {
    alias(libs.plugins.subzero.android.library)
    alias(libs.plugins.subzero.android.room)
    alias(libs.plugins.subzero.hilt)
}

android {
    namespace = "com.subzero.core.data"

    testOptions {
        // DAO and migration tests run on the JVM through Robolectric so they are part of
        // `gradlew test`, not only of instrumented runs.
        unitTests.isIncludeAndroidResources = true
    }

    sourceSets {
        // Exported Room schemas as test assets so MigrationTestHelper can load old versions.
        getByName("test").assets.directories.add("schemas")
    }
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.common)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(testFixtures(projects.core.domain))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(testFixtures(projects.core.domain))
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.runner)
}
