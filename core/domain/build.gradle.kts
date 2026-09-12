// Pure Kotlin/JVM: no Android dependency. Every financial rule lives here and is unit-tested
// with plain JUnit. Domain models, repository interfaces and use cases arrive in Phase 2.
plugins {
    alias(libs.plugins.subzero.jvm.library)
    alias(libs.plugins.subzero.hilt)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
