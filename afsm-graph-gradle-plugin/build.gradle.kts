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
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.13.2")
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
