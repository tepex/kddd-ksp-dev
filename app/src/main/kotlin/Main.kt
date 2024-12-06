package ru.it_arch.clean_ddd.app

import ru.it_arch.clean_ddd.domain.MyStruct
import ru.it_arch.clean_ddd.domain.MyValueObject
import ru.it_arch.clean_ddd.domain.impl.myStruct
import java.net.URI

val testStruct = myStruct {
    name = "my name"
    optName = null
    count = 12
    uri = URI.create("https://ya.ru")
    names += "another name"
}

fun main() {
    val qqq: MyValueObject? = null
    println("clean ddd")
    println("myStruct: $testStruct")
}

class Main {
}
