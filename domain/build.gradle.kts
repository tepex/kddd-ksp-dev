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
    explicitApi()
    compilerOptions {
        freeCompilerArgs = freeCompilerArgs.get() + "-Xcontext-receivers"
    }
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
