plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation(libs.bundles.junit)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.register("checkNoDependencies") {
    doLast {
        val configNames = listOf("implementation", "api", "compileOnly", "runtimeOnly")
        var foundDependencies = false

        for (configName in configNames) {
            val config = configurations.findByName(configName) ?: continue
            // Check explicitly declared dependencies, excluding Kotlin stdlib added by the plugin
            val dependencies = config.dependencies.filter {
                !it.toString().contains("kotlin-stdlib")
            }
            if (dependencies.isNotEmpty()) {
                println("ERROR: poker-engine:$configName has declared dependencies:")
                for (dep in dependencies) {
                    println("  - $dep")
                }
                foundDependencies = true
            }
        }

        if (foundDependencies) {
            throw GradleException("poker-engine should have no production dependencies")
        }
    }
}

tasks.named("check") {
    dependsOn("checkNoDependencies")
}
