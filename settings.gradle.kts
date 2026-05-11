pluginManagement {
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
include(":afsm-compose")
include(":afsm-graph-ksp")
include(":afsm-runtime")
include(":afsm-viewmodel")
include(":sample-shop")
