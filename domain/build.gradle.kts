import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        //apiVersion.set(KotlinVersion.KOTLIN_2_0)
        //languageVersion.set(KotlinVersion.KOTLIN_2_0)
    }
    /*
    sourceSets.all {
        languageSettings.enableLanguageFeature("ExplicitBackingFields")
    }*/
    explicitApi()
}

dependencies {
    implementation(libs.kddd)
    ksp(project(":ksp"))
}

ksp {
    arg("a", "1")
    arg("b", "2")
}
