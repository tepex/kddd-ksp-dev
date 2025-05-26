pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
    }
}


plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include(
    ":lib",
    ":ksp-utils-api",
    ":ksp-utils-impl",
    ":core-domain",
    ":core-data-kotlinpoet",
    ":presentation",
    ":my-cool-domain",
    ":demo",
    ":processor"
)
