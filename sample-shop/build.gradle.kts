plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    kotlin("android")
    kotlin("plugin.compose")
}

android {
    namespace = "afsm.sample.shop"
    compileSdk = 36

    defaultConfig {
        applicationId = "afsm.sample.shop"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
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
    implementation(project(":afsm-core"))
    implementation(project(":afsm-runtime"))
    implementation(project(":afsm-viewmodel"))

    implementation(platform("androidx.compose:compose-bom:2026.03.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.7")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.room:room-ktx:2.8.4")
    implementation("androidx.room:room-runtime:2.8.4")
    ksp(project(":afsm-graph-ksp"))
    ksp("androidx.room:room-compiler:2.8.4")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty(
        "afsm.mmd.outputDir",
        layout.buildDirectory.dir("generated/afsm/mmd").get().asFile.absolutePath,
    )
    outputs.dir(layout.buildDirectory.dir("generated/afsm/mmd"))
}

tasks.register("generateAfsmMmd") {
    group = "documentation"
    description = "Generates Afsm state machine .mmd graph files."
    dependsOn("testDebugUnitTest")
    outputs.dir(layout.buildDirectory.dir("generated/afsm/mmd"))
}
