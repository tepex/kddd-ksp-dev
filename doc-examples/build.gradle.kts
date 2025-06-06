import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

kotlin {
    jvmToolchain(19)
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
        //freeCompilerArgs = freeCompilerArgs.get() + "-Xcontext-rceivers"
    }
}

dependencies {
    implementation(project(":lib"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
}

val MAIN_CLASS = "ru.it_arch.kddd.magic.MainKt"

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = MAIN_CLASS
    }
}

application {
    mainClass = MAIN_CLASS
}
