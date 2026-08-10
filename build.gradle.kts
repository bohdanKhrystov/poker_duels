plugins {
    java
    kotlin("jvm") version libs.versions.kotlin apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// The version catalog accessor is only wired to the root project's own extension
// container; capture the value here so it can be reused inside `subprojects { }`.
val ktlintToolVersion = libs.versions.ktlint.get()

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    // Configure ktlint with official Kotlin style
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set(ktlintToolVersion)
    }

    // Configure detekt
    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom("${rootProject.projectDir}/config/detekt/detekt.yml")
    }

    // detekt 1.23.1's `--jvm-target` flag tops out at 20, so pin it below the project's
    // toolchain (21) to keep static analysis runnable.
    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        jvmTarget = "17"
    }

    // Wire ktlint and detekt into check. The `check` task itself is only registered once
    // the subproject applies its own language plugin (e.g. kotlin("jvm")), so defer until
    // project evaluation has finished.
    afterEvaluate {
        tasks.named("check") {
            dependsOn("ktlintCheck")
            dependsOn("detekt")
        }
    }
}
