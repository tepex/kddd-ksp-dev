package ru.it_arch.clean_ddd.ksp.interop

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspIncremental
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.kspWithCompilation
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.reflection.shouldBeData
import io.kotest.matchers.reflection.shouldHaveFunction
import io.kotest.matchers.reflection.shouldHaveMemberProperty
import io.kotest.matchers.reflection.shouldHavePrimaryConstructor
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import ru.it_arch.clean_ddd.ksp.DddProcessorProvider
import java.io.File
import kotlin.reflect.KClass

@OptIn(ExperimentalCompilerApi::class)
class FirstTest : FunSpec({

    val testingClassName = "MySimple"

    val source = SourceFile.kotlin("$testingClassName.kt",
"""

public sealed interface ValueObject {

    /** For `data class` */
    public interface Data : ValueObject, Validatable

    /** For `value class` */
    public interface Boxed<BOXED : Any> : ValueObject, Validatable {
        public val boxed: BOXED
        public fun <T : Boxed<BOXED>> copy(boxed: BOXED): T
    }

    /** For `enum class`, `sealed interface` */
    public interface Sealed : ValueObject
}


public interface $testingClassName : ValueObject.Data {
    public val name: Name

    override fun validate() {   }

    public interface Name : ValueObject.Boxed<String> {

        override fun validate() {   }
    }
}
""")

    val kc = KotlinCompilation().apply {
        sources = listOf(source)
        symbolProcessorProviders = listOf(DddProcessorProvider(true))
        inheritClassPath = false
        kspIncremental = false
        verbose = true
        kotlinStdLibJar = File("/Users/user/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/1.9.0/8ee15ef0c67dc83d874f412d84378d7f0eb50b63/kotlin-stdlib-1.9.0.jar")
        messageOutputStream = System.out // see diagnostics in real time
    }
    val result = kc.compile()

    pos("Compilation") {
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        println("generated: ${kc.kspSourcesDir.list().toList()}")

    }

    context("$testingClassName ->") {
        val clazz = result.classLoader.loadClass("`impl`.${testingClassName}Impl").kotlin

        /*
        pos("Есть пара name") {
            clazz.shouldHaveMemberProperty("name")
        }*/

        pos("Тип `data`") {
            clazz.shouldBeData()
        }

        pos("Первичный конструктор") {
            clazz.shouldHavePrimaryConstructor()
        }

        pos("Функция `toBuilder()`") {
            clazz.shouldHaveFunction("toBuilder")
        }

        pos("Функция `toDslBuilder()`") {
            clazz.shouldHaveFunction("toDslBuilder")
        }
    }
})
