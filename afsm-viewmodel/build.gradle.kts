plugins {
    id("com.android.library")
    kotlin("android")
    `maven-publish`
}

android {
    namespace = "afsm.viewmodel"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":afsm-runtime"))
    api("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "afsm-viewmodel"

                pom {
                    name.set("Afsm ViewModel")
                    description.set("Thin AndroidX ViewModel integration for AfsmHost and viewModelScope.")
                }
            }
        }
    }
}
