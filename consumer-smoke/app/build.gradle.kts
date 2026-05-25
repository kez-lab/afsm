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
    implementation("io.github.afsm:afsm-compose:$afsmVersion")
    implementation("io.github.afsm:afsm-runtime:$afsmVersion")
    implementation("io.github.afsm:afsm-viewmodel:$afsmVersion")

    testImplementation("io.github.afsm:afsm-test:$afsmVersion")
    testImplementation(kotlin("test-junit"))
}
