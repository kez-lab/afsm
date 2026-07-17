plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
}

kotlin {
    jvmToolchain(17)
    explicitApi()
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    api(project(":afsm-core"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "afsm-runtime"

            pom {
                name.set("Afsm Runtime")
                description.set("Coroutine Afsm host with serialized dispatch, command execution, and diagnostics.")
            }
        }
    }
}
