plugins {
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("io.github.afsm.graph")
    kotlin("android")
}

val afsmVersion = providers.gradleProperty("afsmVersion")
    .orElse("0.1.0-SNAPSHOT")
    .get()

android {
    namespace = "afsm.consumer.smoke"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("io.github.afsm:afsm-core:$afsmVersion")
    implementation("io.github.afsm:afsm-runtime:$afsmVersion")
    implementation("io.github.afsm:afsm-viewmodel:$afsmVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.10.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.github.afsm:afsm-test:$afsmVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
