pluginManagement {
    val afsmVersion = providers.gradleProperty("afsmVersion")
        .orElse("0.1.0-SNAPSHOT")

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.github.afsm.graph") {
                useVersion(afsmVersion.get())
            }
        }
    }

    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "afsm-consumer-smoke"

include(":app")
