import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
}

group = "io.github.afsm"
version = "0.1.1-SNAPSHOT"

dependencies {
    implementation("org.eclipse.elk:org.eclipse.elk.alg.layered:0.12.0")

    intellijPlatform {
        intellijIdea("2026.1.4")
        bundledPlugin("com.intellij.gradle")
        bundledPlugin("org.jetbrains.kotlin")
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation(kotlin("test-junit"))
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
    }
}

intellijPlatform {
    buildSearchableOptions = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "261"
        }
    }

    pluginVerification {
        ides {
            create(IntelliJPlatformType.IntellijIdea, "2026.1.4")
            create(IntelliJPlatformType.AndroidStudio, "2026.1.3.8")
        }
    }
}

tasks.test {
    useJUnit()
}

tasks.processResources {
    from("THIRD_PARTY_NOTICES.md") {
        into("META-INF")
    }
}

val runAndroidStudio by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.AndroidStudio
    version = "2026.1.3.8"
    task {
        args(projectDir.parentFile.absolutePath)
        jvmArgs(
            "-Dide.mac.message.dialogs.as.sheets=false",
            "-Djb.privacy.policy.text=<!--999.999-->",
            "-Djb.consents.confirmation.enabled=false",
        )
    }
    plugins {
        bundledPlugin("com.intellij.gradle")
        bundledPlugin("org.jetbrains.kotlin")
    }
}

val testAndroidStudio by intellijPlatformTesting.testIde.registering {
    type = IntelliJPlatformType.AndroidStudio
    version = "2026.1.3.8"
    testFramework(TestFrameworkType.Platform)
    task {
        useJUnit()
    }
    plugins {
        bundledPlugin("com.intellij.gradle")
        bundledPlugin("org.jetbrains.kotlin")
    }
}
