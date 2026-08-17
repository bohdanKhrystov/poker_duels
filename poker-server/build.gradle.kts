plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":poker-engine"))
    implementation(libs.bouncycastle.provider)
    implementation(libs.bundles.ktor.server)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    runtimeOnly(libs.logback.classic)
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

tasks.register<JavaExec>("verifyProtocolTypes") {
    group = "protocol"
    description = "Fails the build if web-client/src/protocol/protocol.gen.ts drifts from the protocol serial descriptors (ADR-0020)."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("duels.poker.server.protocol.typescript.GenerateProtocolTypesKt")

    // Plain File locals, resolved at configuration time, so the doLast action below
    // reaches back into neither Project nor this task — required for the configuration
    // cache, and so this task regenerates into the build directory rather than touching
    // the committed file it is comparing against.
    val generatedFile = layout.buildDirectory.file("protocol/protocol.gen.ts").get().asFile
    val committedFile = rootProject.file("web-client/src/protocol/protocol.gen.ts")
    args(generatedFile.absolutePath)

    doLast {
        if (!generatedFile.readBytes().contentEquals(committedFile.readBytes())) {
            throw GradleException(
                "web-client/src/protocol/protocol.gen.ts is out of date. " +
                    "Run ./gradlew :poker-server:generateProtocolTypes and commit the result.",
            )
        }
    }
}

tasks.register<JavaExec>("generateDuelScript") {
    group = "protocol"
    description = "Emits web-client/src/e2e/scripted-duel.gen.json from ScriptedDuel (TASK-031203). " +
        "Classpath is the test runtime, not main: playDuel and scriptedDuel are test scaffolding " +
        "and must never reach the production jar."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("duels.poker.server.duel.GenerateDuelScriptKt")
    args(rootProject.file("web-client/src/e2e/scripted-duel.gen.json").absolutePath)
}

tasks.register<JavaExec>("verifyDuelScript") {
    group = "protocol"
    description = "Fails the build if web-client/src/e2e/scripted-duel.gen.json drifts from ScriptedDuel " +
        "(TASK-031203). Classpath is the test runtime, not main, for the same reason generateDuelScript's is."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("duels.poker.server.duel.GenerateDuelScriptKt")

    // Plain File locals, resolved at configuration time, so the doLast action below
    // reaches back into neither Project nor this task — required for the configuration
    // cache, and so this task regenerates into the build directory rather than touching
    // the committed file it is comparing against.
    val generatedFile = layout.buildDirectory.file("e2e/scripted-duel.gen.json").get().asFile
    val committedFile = rootProject.file("web-client/src/e2e/scripted-duel.gen.json")
    args(generatedFile.absolutePath)

    doLast {
        if (!generatedFile.readBytes().contentEquals(committedFile.readBytes())) {
            throw GradleException(
                "web-client/src/e2e/scripted-duel.gen.json is out of date. " +
                    "Run ./gradlew :poker-server:generateDuelScript and commit the result.",
            )
        }
    }
}

tasks.named("check") {
    dependsOn("verifyProtocolTypes")
    dependsOn("verifyDuelScript")
}
