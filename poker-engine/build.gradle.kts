plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation(libs.bundles.junit)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
