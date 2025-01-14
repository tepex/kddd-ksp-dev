plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
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
    implementation(libs.kotlinx.serialization.json)
    ksp(project(":ksp-impl"))
}

ksp {
    arg("subpackage", "impl")
    arg("contextReceivers", "true")
    arg("serialNameCase", "kebab")
    //arg("generatedClassNameResult", "$1Default")
}
