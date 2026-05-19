import java.util.Properties

plugins {
    kotlin("jvm") version "2.0.21"
    `java-gradle-plugin`
    `maven-publish`
}

val afsmVersion = Properties()
    .also { properties ->
        file("../gradle.properties").inputStream().use(properties::load)
    }
    .getProperty("afsmVersion")
    ?: error("Missing afsmVersion in ../gradle.properties")

group = "io.github.afsm"
version = afsmVersion

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

tasks.processResources {
    inputs.property("afsmVersion", afsmVersion)
    filesMatching("afsm/gradle/afsm-graph-plugin.properties") {
        expand("afsmVersion" to afsmVersion)
    }
}
