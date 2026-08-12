plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.bundles.junit)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotlinx.coroutines.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.register("checkNoDependencies") {
    val configNames = listOf("implementation", "api", "compileOnly", "runtimeOnly")

    // ADR-0010: the allowlist is narrowed, never removed. Adding to it needs a new ADR.
    val allowedDependencies = listOf("kotlin-stdlib", "kotlinx-serialization")

    // Capture dependency information during configuration phase
    val configsWithUnwantedDeps = mutableMapOf<String, List<String>>()

    for (configName in configNames) {
        val config = configurations.findByName(configName) ?: continue
        val unwantedDeps = config.dependencies
            .filter { dep -> allowedDependencies.none { dep.toString().contains(it) } }
            .map { it.toString() }

        if (unwantedDeps.isNotEmpty()) {
            configsWithUnwantedDeps[configName] = unwantedDeps
        }
    }

    // Store as task input for configuration cache safety
    inputs.property("configsWithUnwantedDeps", configsWithUnwantedDeps)

    doLast {
        @Suppress("UNCHECKED_CAST")
        val problemConfigs = inputs.getProperties()["configsWithUnwantedDeps"] as Map<String, List<String>>

        if (problemConfigs.isNotEmpty()) {
            for ((configName, dependencies) in problemConfigs) {
                println("ERROR: poker-engine:$configName has declared dependencies:")
                for (dep in dependencies) {
                    println("  - $dep")
                }
            }
            throw GradleException("poker-engine should have no production dependencies")
        }
    }
}

tasks.named("check") {
    dependsOn("checkNoDependencies")
}
