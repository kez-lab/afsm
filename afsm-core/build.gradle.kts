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
    testImplementation(kotlin("test-junit5"))
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "afsm-core"

            pom {
                name.set("Afsm Core")
                description.set("Pure Kotlin Afsm transition types, reducer contract, executable machine DSL, and graph metadata.")
            }
        }
    }
}
