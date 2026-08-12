plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":poker-engine"))
    testImplementation(libs.bundles.junit)
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("soak")
    }
}

tasks.register<Test>("soakTest") {
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("soak")
    }
    systemProperty("soakDuels", providers.gradleProperty("soakDuels").getOrElse("100000"))
}
