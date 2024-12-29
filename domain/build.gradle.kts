import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(17)
    /*
    sourceSets.all {
        languageSettings.enableLanguageFeature("ExplicitBackingFields")
    }*/
    compilerOptions {
        freeCompilerArgs = freeCompilerArgs.get() + "-Xcontext-receivers"
    }
    explicitApi()
}

dependencies {
    implementation(libs.kddd)
    ksp(project(":ksp-impl"))
}

ksp {
    arg("subpackage", "impl")
    arg("contextReceivers", "true")
    //arg("generatedClassNameResult", "$1Default")
}
