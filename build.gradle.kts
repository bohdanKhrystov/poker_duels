plugins {
    java
    kotlin("jvm") version libs.versions.kotlin apply false
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
