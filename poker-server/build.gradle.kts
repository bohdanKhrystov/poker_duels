plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":poker-engine"))
    implementation(libs.bundles.ktor.server)
    testImplementation(libs.bundles.junit)
    testImplementation(libs.ktor.server.test.host)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
