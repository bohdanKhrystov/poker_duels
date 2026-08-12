plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":poker-engine"))
    implementation(libs.bundles.ktor.server)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.bundles.junit)
    testImplementation(libs.ktor.server.test.host)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
