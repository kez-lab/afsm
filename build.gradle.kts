plugins {
    id("com.android.application") version "8.10.1" apply false
    id("com.android.library") version "8.10.1" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.18.1"
    kotlin("android") version "2.0.21" apply false
    kotlin("plugin.compose") version "2.0.21" apply false
    kotlin("jvm") version "2.0.21" apply false
}

val afsmVersion = providers.gradleProperty("afsmVersion")
    .get()

allprojects {
    group = "io.github.afsm"
    version = afsmVersion
}

apiValidation {
    ignoredProjects.add("sample-shop")
}
