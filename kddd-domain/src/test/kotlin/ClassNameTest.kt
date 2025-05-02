package ru.it_arch.clean_ddd.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID

class ClassNameTest : FunSpec({
    pos("'String' class name must be 'ClassName.Name.Primitive.STRING'") {
        "String".toBoxedClassName() shouldBe className { name = ClassName.Name.Primitive.STRING }
    }

    pos("'UUID' class name must be 'ClassName.Name.Common'") {
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
