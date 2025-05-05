package ru.it_arch.clean_ddd.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class OptionsTest : FunSpec({

    pos("Default options must generate '<MyType>Impl' class name") {
        with(options { }) {
            "MyType".`to implementation class name` shouldBe "MyTypeImpl"
        }
    }

    pos("""Options with RE 'I(\D+)([\d]+)Test' and result template: '$1_$2_Impl'  must generate 'MyType_33_Impl' class name from 'IMyType33Test' source class name""") {
        with(options {
            generatedClassNameRe = "I(\\D+)([\\d]+)Test"
            generatedClassNameResult = "$1_$2_Impl"
        }) {
            "IMyType33Test".`to implementation class name` shouldBe "MyType_33_Impl"
        }
    }

    pos(""" "<package name>".toImplementationPackage must be added with 'Options.subpackage'""") {
        val basePackage = "com.example.domain"
        val impl = "test"
        with(options { subpackage = impl }) {
            basePackage.toImplementationPackage shouldBe "$basePackage.$impl"
        }
    }
})
