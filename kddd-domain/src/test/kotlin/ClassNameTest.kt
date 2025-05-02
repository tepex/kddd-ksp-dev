package ru.it_arch.clean_ddd.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID

class ClassNameTest : FunSpec({

    context("""'"<Primitive>".toBoxedClassName()' must return appropriate """) {
        withData(
            nameFn = { (src, _) -> "'ClassName(name = ClassName.Name.Primitive.${src.uppercase()})'" },
            ts = ClassName.Name.Primitive.entries
                .map { primitive -> primitive.name.lowercase().replaceFirstChar { it.uppercaseChar() } to className { name = primitive } }
        ) { (primitive, className) ->  primitive.toBoxedClassName() shouldBe className }
    }

    pos("""'UUID.toBoxedClassName()' must return 'ClassName(name = ClassName.Name.Common("UUID"))'""") {
        "UUID".toBoxedClassName() shouldBe className { name = ClassName.Name.Common(UUID::class.java.simpleName) }
    }

    pos("""'A.B.C' class name must be ClassName("C").enclosing -> ClassName("B").enclosing -> ClassName("A").enclosing -> null""") {
        val a = className { name = ClassName.Name.KdddType("A") }
        val b = className {
            name = ClassName.Name.KdddType("B")
            enclosing = a
        }
        val c = className {
            name = ClassName.Name.KdddType("C")
            enclosing = b
        }

        "A.B.C".toKddClassName() shouldBe c
    }

    neg("""'A.X.C' class name must not be ClassName("C").enclosing -> ClassName("B").enclosing -> ClassName("A").enclosing -> null""") {
        val a = className { name = ClassName.Name.KdddType("A") }
        val b = className {
            name = ClassName.Name.KdddType("B")
            enclosing = a
        }
        val c = className {
            name = ClassName.Name.KdddType("C")
            enclosing = b
        }

        "A.X.C".toKddClassName() shouldNotBe c
    }

})
