import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
    }
    explicitApi()
}

dependencies {
    implementation(libs.ksp.api)
    implementation(project(":core-domain"))
}
