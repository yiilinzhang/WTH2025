pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // Only settings repositories
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "WhatTheHack"
include(":app")
