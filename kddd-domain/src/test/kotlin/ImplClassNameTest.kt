package ru.it_arch.clean_ddd.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import ru.it_arch.kddd.KDGeneratable

class ImplClassNameTest : FunSpec({

    pos("ClassNameImpl must be as '@KDGeneratable.implementationName'") {
        val resultClassName = "MyTestImpl"
        val annotations = listOf(KDGeneratable(implementationName = resultClassName))
        with(options {}) {
            "MyType" `to implementation class name with @KDGeneratable annotation in` annotations shouldBe
                KdddType.Generatable.ImplClassName(resultClassName)
        }

    }
})
