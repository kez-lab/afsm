pluginManagement {
    includeBuild("afsm-graph-gradle-plugin")

    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "afsm"

include(":afsm-core")
include(":afsm-graph-ksp")
include(":afsm-runtime")
include(":afsm-test")
include(":afsm-viewmodel")
include(":sample-shop")
