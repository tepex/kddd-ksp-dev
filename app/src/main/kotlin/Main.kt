package ru.it_arch.clean_ddd.app

import ru.it_arch.clean_ddd.domain.MySimple
import ru.it_arch.clean_ddd.domain.impl.MyStructImpl
import ru.it_arch.clean_ddd.domain.impl.abstr
import ru.it_arch.clean_ddd.domain.impl.mySimple
import ru.it_arch.clean_ddd.domain.impl.myStruct
import ru.it_arch.clean_ddd.domain.impl.point
import ru.it_arch.clean_ddd.domain.plus
import ru.it_arch.clean_ddd.domain.serialize
import ru.it_arch.clean_ddd.domain.toPoint
import java.util.UUID

val testStruct = myStruct {
    name = "my name"
    optName = null
    count = 12
    uri = "https://ya.ru"
    names += "another name"
    nullableNames += listOf(null, null)
    indexes += 2
    myMap[3] = null
    myMap[4] = "asdf"
    inner {
        innerLong = 33
        innerStr = "fsdfsd"
    }
    innerList += inner {
        innerLong = 11
        innerStr = "11"
    }
    innerList += inner {
        innerLong = 22
        innerStr = "22"
    }
    innerList.add(null)
}

fun main() {
    abstr1()

    val simple = mySimple {
        name = "simple"
        uri = "https://ya.ru"
        file = "~/.bashrc"
        listUri += "http://google.com"
        nullableListUri += null
        mapUUID["my uuid"] = UUID.randomUUID().toString()
        myEnum = MySimple.MyEnum.A
    }
    println("mySimple: $simple, file: ${simple.file.boxed.canonicalFile}")
    println()
    println()
    println("myStruct: $testStruct")
    //val newCount = testStruct.count.inc()
    /*
    val cp = testStruct.copy(count = testStruct.count + 3)
    println("myStruct copy: $cp")*/

    val cp = testStruct.toBuilder().apply {
        name = MyStructImpl.NameImpl.create("nnnn")
    }.build()
    //println("new struct: ${cp.updateCount()}")

    val point1 = point {
        x = 12
        y = 15
        //en = Point.MyEnum.A
    }

    val point2 = point {
        x = 10
        y = 20
        //en = Point.MyEnum.B
    }
    val sum = point1.plus(point2)
    println()
    println()
    println("point1: $point1, point2: $point2, sun: ${point1 + point2}")
    println("point serialize: ${point1.serialize()}")
    val point = "3:5".toPoint()
    println("point: $point")
}

class Main {
}


fun abstr1() {
    val abs = abstr {
        name = "Abstr"
        //some = AbstrImpl.NameImpl.create("qqq")
        //someList += AbstrImpl.NameImpl.create("qqq")
    }

    println("abstr = $abs")
}
