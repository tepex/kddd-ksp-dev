package ru.it_arch.kddd.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import ru.it_arch.kddd.domain.internal.`to implementation class name from options`
import ru.it_arch.kddd.domain.internal.toImplementationPackage
import ru.it_arch.kddd.domain.model.CompositeClassName

class OptionsTest : FunSpec({

    pos("Default options must generate '<MyType>Impl' class name") {
        with(options { }) {
            CompositeClassName.ClassName("MyType").`to implementation class name from options` shouldBe
                CompositeClassName.ClassName("MyTypeImpl")
        }
    }

    pos("""Options with RE 'I(\D+)([\d]+)Test' and result template: '$1_$2_Impl' must generate 'MyType_33_Impl' class name from 'IMyType33Test' source class name""") {
        with(options {
            generatedClassNameRe = "I(\\D+)([\\d]+)Test"
            generatedClassNameResult = "$1_$2_Impl"
        }) {
            CompositeClassName.ClassName("IMyType33Test").`to implementation class name from options` shouldBe
                CompositeClassName.ClassName("MyType_33_Impl")
        }
    }

    pos(""" "<package name>".toImplementationPackage must be added with 'Options.subpackage'""") {
        val basePackage = CompositeClassName.PackageName("com.example.domain")
        val impl = "test"
        with(options { subpackage = impl }) {
            basePackage.toImplementationPackage shouldBe CompositeClassName.PackageName("$basePackage.$impl")
        }
    }
})
