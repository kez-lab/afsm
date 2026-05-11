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
    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.21-1.0.28")

    testImplementation(kotlin("test-junit5"))
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "afsm-graph-ksp"

            pom {
                name.set("Afsm Graph KSP")
                description.set("KSP processor that discovers Afsm graph sources and generates graph registries.")
            }
        }
    }
}
