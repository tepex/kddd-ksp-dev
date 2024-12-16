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
}

dependencies {
    implementation(libs.kddd)
    ksp(project(":ksp-impl"))
}

ksp {
    arg("subpackage", "impl")
    arg("generatedClassNameResult", "$1Default")
}
