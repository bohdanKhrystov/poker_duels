plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":poker-engine"))
    implementation(libs.bundles.ktor.server)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    testImplementation(libs.bundles.junit)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.testcontainers.postgresql)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // -PrequireDocker=true turns a missing Docker daemon from a skipped test into a
    // failing build. TASK-020903 reads it; CI passes it.
    systemProperty("poker.requireDocker", providers.gradleProperty("requireDocker").getOrElse("false"))
}
