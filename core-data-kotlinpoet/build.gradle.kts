import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
        //freeCompilerArgs = freeCompilerArgs.get() + "-Xcontext-receivers"
        freeCompilerArgs = freeCompilerArgs.get() + "-Xcontext-parameters"
    }
    /*
    sourceSets.all {
        languageSettings.enableLanguageFeature("ExplicitBackingFields")
    }*/
    explicitApi()
}

dependencies {
    implementation(project(":kddd"))
    implementation(project(":core-domain"))
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
}
