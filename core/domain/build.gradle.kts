// Pure Kotlin/JVM: no Android dependency. Every financial rule lives here and is unit-tested
// with plain JUnit. Test fixtures (fakes, builders, fixed clocks) are shared with other modules
// via testFixtures(projects.core.domain).
plugins {
    alias(libs.plugins.subzero.jvm.library)
    alias(libs.plugins.subzero.hilt)
    `java-test-fixtures`
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    testFixturesImplementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
