plugins {
    kotlin("jvm")
    `java-library`
}

kotlin {
    jvmToolchain(17)
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
