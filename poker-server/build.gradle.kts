plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":poker-engine"))
    implementation(libs.bundles.ktor.server)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    testImplementation(libs.bundles.junit)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.websockets)
    testImplementation(libs.testcontainers.postgresql)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // -PrequireDocker=true turns a missing Docker daemon from a skipped test into a
    // failing build. TASK-020903 reads it; CI passes it.
    systemProperty("poker.requireDocker", providers.gradleProperty("requireDocker").getOrElse("false"))
    // Docker Engine 29+ rejects client API versions below 1.40. Requesting 1.41 is the
    // lowest compatible version that works with both modern and older daemons. Omitting
    // this causes Testcontainers to report "could not find a valid Docker environment",
    // which is actively misleading: the daemon was found and answered, only the version
    // was wrong. The absence of this property makes that failure unrecognisable.
    systemProperty("api.version", "1.41")
}

tasks.register<JavaExec>("generateProtocolTypes") {
    group = "protocol"
    description = "Emits web-client/src/protocol/protocol.gen.ts from the protocol serial descriptors (ADR-0020)."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("duels.poker.server.protocol.typescript.GenerateProtocolTypesKt")
    args(rootProject.file("web-client/src/protocol/protocol.gen.ts").absolutePath)
}
