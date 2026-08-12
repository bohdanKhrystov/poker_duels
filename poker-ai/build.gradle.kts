plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":poker-engine"))
    testImplementation(libs.bundles.junit)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
