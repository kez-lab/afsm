pluginManagement {
    val afsmVersion = providers.gradleProperty("afsmVersion")
        .orElse("0.1.0")
    val useMavenLocal = providers.gradleProperty("useMavenLocal")
        .map(String::toBoolean)
        .orElse(true)

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.github.afsm.graph") {
                useVersion(afsmVersion.get())
            }
        }
    }

    repositories {
        if (useMavenLocal.get()) {
            mavenLocal()
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    val useMavenLocal = providers.gradleProperty("useMavenLocal")
        .map(String::toBoolean)
        .orElse(true)

    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (useMavenLocal.get()) {
            mavenLocal()
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "afsm-consumer-smoke"

include(":app")
