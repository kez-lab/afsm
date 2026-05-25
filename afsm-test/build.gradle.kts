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
    api(kotlin("test"))

    testImplementation(kotlin("test-junit5"))
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "afsm-test"

            pom {
                name.set("Afsm Test")
                description.set("Kotlin test helpers for asserting Afsm transition behavior.")
            }
        }
    }
}
