plugins {
    kotlin("jvm") version "2.0.21"
    `java-gradle-plugin`
    `maven-publish`
}

group = "io.github.afsm"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly("com.android.tools.build:gradle:8.10.1")
}

gradlePlugin {
    plugins {
        create("afsmGraph") {
            id = "io.github.afsm.graph"
            implementationClass = "afsm.gradle.AfsmGraphPlugin"
            displayName = "Afsm Graph"
            description = "Generates Afsm Mermaid state diagrams from @AfsmGraph machines."
        }
    }
}
