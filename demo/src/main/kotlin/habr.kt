package ru.it_arch.clean_ddd.app

import ru.it_arch.clean_ddd.domain.demo.incrementCount
import ru.it_arch.clean_ddd.domain.habr.impl.myType
import java.util.UUID

fun myTypeExample() {
    // Создание через DSL
    val myType = myType {
        name = "my name"
        count = 3
        components += UUID.randomUUID() to "another name"
    }
    println("myType: $myType")
    myType.incrementCount().also { println("Incremented MyType: $it") }
}
