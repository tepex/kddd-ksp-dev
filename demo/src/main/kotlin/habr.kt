package ru.it_arch.clean_ddd.app

import ru.it_arch.clean_ddd.domain.habr.impl.myType
import java.util.UUID

fun myTypeExample() {
    val myType = myType {
        name = "my name"
        count = 3
        components += UUID.randomUUID() to "another name"
    }
    println("myType: $myType")
}
