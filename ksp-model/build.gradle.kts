import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    /*
    alias(libs.plugins.test.report)
    alias(libs.plugins.kover)*/
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

/*
tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    kotlinOptions.freeCompilerArgs += "-Xcontext-receivers"
}*/

/*
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    /*
    testLogging {
        events("passed", "skipped", "failed")
    }*/
}*/

dependencies {
    implementation(libs.kddd)
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kddd)
    testImplementation(libs.kotlin.compile.testing.ksp)

    /*
    testImplementation(libs.kotest)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.datatest)

     */
}

/*
kover {
    reports {
        filters {
            includes {
                packages("ru.it_arch.clean_ddd.ksp.interop")
            }
        }
        total {
            html {
                onCheck = true
            }
            verify {
                rule {
                    disabled = false
                    bound {
                        minValue = 1
                    }
                }
            }
        }
    }
}

*/
