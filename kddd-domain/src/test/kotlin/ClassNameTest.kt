package ru.it_arch.clean_ddd.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import ru.it_arch.clean_ddd.domain.core.BoxedWithCommon
import ru.it_arch.clean_ddd.domain.core.BoxedWithPrimitive
import ru.it_arch.kddd.KDParsable
import java.util.UUID

class ClassNameTest : FunSpec({

    val generatable = generatable {
        kddd = compositeClassName {
            packageName = "not used"
            fullClassName = "not used"
        }
        implClassName = CompositeClassName.ClassName("not used")
        implPackageName = CompositeClassName.PackageName("not used")
    }

    val options = options {  }

    val defaultContext = with(options) {
        kDddContext {
            kddd = compositeClassName {
                packageName = "not used"
                fullClassName = "not used"
            }
            properties = emptyList()
        }
    }

    context("""'"<Primitive>".toBoxedTypeWith()' must return appropriate """) {
        withData(
            nameFn = { (src, _) -> "'BoxedWithPrimitive(...${src.uppercase()})'" },
            ts = BoxedWithPrimitive.PrimitiveClassName.entries
                .map { primitive ->
                    primitive.name.lowercase().replaceFirstChar { it.uppercaseChar() } to BoxedWithPrimitive(generatable, primitive)
                }
        ) { (primitive, boxed) ->
            with(options) {
                with(defaultContext) {
                    primitive toBoxedTypeWith generatable shouldBe boxed
                }
            }
        }
    }

    pos("""'"UUID".toBoxedTypeWith()' must return 'BoxedWithCommon(boxed = CommonClassName("UUID"))'""") {
        with(options) {
            with(defaultContext) {
                "UUID" toBoxedTypeWith generatable shouldBe BoxedWithCommon(
                    generatable,
                    BoxedWithCommon.CommonClassName(UUID::class.java.simpleName),
                    KDParsable()
                )
            }
        }
    }

    /*
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
    }*/
})
