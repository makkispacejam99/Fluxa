@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google() // Simplificado para evitar bloqueos de contenido
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") } // Añadido aquí también
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Fluxa"
include(":app")