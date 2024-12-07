package ru.it_arch.clean_ddd.app

import ru.it_arch.clean_ddd.domain.MyStruct
import ru.it_arch.clean_ddd.domain.MyValueObject
import ru.it_arch.clean_ddd.domain.impl.MyStructImpl
import ru.it_arch.clean_ddd.domain.impl.myStruct
import java.net.URI

val testStruct = myStruct {
    name = "my name"
    optName = null
    count = 12
    uri = URI.create("https://ya.ru")
    names += "another name"
    nullableNames = listOf(null, null)
    indexes += 2
    myMap = mapOf(3 to null, 4 to "asdf")
    inner {
        innerLong = 33
        innerStr = "fsdfsd"
    }
}

fun main() {
    println("myStruct: $testStruct")
    //val newCount = testStruct.count.inc()
    val cp = testStruct.copy(count = testStruct.count + 3)
    println("myStruct copy: $cp")
    val newLong = testStruct.inner.innerLong.dec()
}

class Main {
}
