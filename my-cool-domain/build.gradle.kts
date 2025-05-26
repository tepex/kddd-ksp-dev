plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    //alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
    /*
    sourceSets.all {
        languageSettings.enableLanguageFeature("ExplicitBackingFields")
    }*/
    compilerOptions {
        freeCompilerArgs = freeCompilerArgs.get() + "-Xcontext-parameters"
    }
    explicitApi()
}

dependencies {
    implementation(project(":lib"))
    //implementation(libs.kotlinx.serialization.json)
    ksp(project(":processor"))
}

ksp {
    arg("implementationSubpackage", "impl")
    //arg("contextParameters", "true")
    arg("jsonNamingStrategy", "kebab")
    //arg("generatedClassNameResult", "$1Default")
}
