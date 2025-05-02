package ru.it_arch.clean_ddd.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ClassNameTest : FunSpec({
    pos("first test") {
        "qqq" shouldBe "qqq"
    }
})
