// Pure Kotlin/JVM: every money rule lives here, with no Android dependency, so it runs (and is
// tested) on the plain JVM — the Android twin of web/'s "pure module + thin I/O wrapper" split.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    // Same pin as web/vitest.config.ts: every period/day boundary is computed in local time.
    systemProperty("user.timezone", "Asia/Kolkata")
    // Golden fixtures generated from the TS implementation (cd web && npm run gen:fixtures).
    systemProperty("finio.fixtures", rootProject.file("../spec/fixtures").absolutePath)
    testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}
