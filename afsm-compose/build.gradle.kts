plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("plugin.compose")
    `maven-publish`
}

android {
    namespace = "afsm.compose"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

kotlin {
    explicitApi()
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    api(platform("androidx.compose:compose-bom:2026.03.00"))
    api("androidx.compose.runtime:runtime")
    api("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "afsm-compose"

                pom {
                    name.set("Afsm Compose")
                    description.set("Lifecycle-aware Compose helpers for collecting Afsm effects.")
                }
            }
        }
    }
}
