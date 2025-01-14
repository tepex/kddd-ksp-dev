package ru.it_arch.clean_ddd.app

import com.squareup.kotlinpoet.asTypeName
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import ru.it_arch.clean_ddd.domain.MySimpleJson
import ru.it_arch.clean_ddd.domain.impl.MyStructImpl
import ru.it_arch.clean_ddd.domain.impl.abstr
import ru.it_arch.clean_ddd.domain.impl.myStruct
import ru.it_arch.clean_ddd.domain.impl.point
import ru.it_arch.clean_ddd.domain.mySimpleJson
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
    inner = inner {
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

const val mySimpleJsonStr = """
    {
    "name-name": "json simple",
    "count": 33,
    "uri": "https://json.ru",
    "list-uri": [
        "http://json.com"
    ],
    "nullable-list-uri": [
        null,
        "http://json.ru"
    ],
    "file": "~/.bashrc.tmp",
    "uuid": null,
    "map-uuid": {
        "my uuid": "68792b96-2a27-4336-9415-35d78c8f0903"
    },
    "nested-list1": [
        [
            "_set1",
            "_set2"
        ],
        [
            "_set11",
            "_set12"
        ]
    ],
    "nested-map": {
        "_key.name": [
            "_n1",
            "_n2",
            "_n3"
        ]
    }
}"""

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    prettyPrint = true
    namingStrategy = JsonNamingStrategy.KebabCase
}

fun main() {
    //abstr1()
    val qqq = Int::class.asTypeName()
    val simple = mySimpleJson {
        nameName = "simple"
        count = 77
        uri = "https://ya.ru"
        file = "~/.bashrc"
        listUri += "http://google.com"
        nullableListUri += null
        nullableListUri += "http://ya.ru"
        mapUUID["my uuid"] = UUID.randomUUID()
        //myEnum = MySimple.MyEnum.A
        nestedList1 += mutableSetOf("set1", "set2")
        nestedList1 += mutableSetOf("set11", "set12")
        nestedMap["key.name"] = mutableListOf("n1", "n2", "n3")
    }
    println("mySimple: $simple, file: ${simple.file.boxed.canonicalFile}")

    val jsonStr = json.encodeToString(simple)
    println("json: $jsonStr")

    val obj = json.decodeFromString<MySimpleJson>(mySimpleJsonStr)
    println("deserialize: $obj")



    if (true) return


    //println("myStruct: $testStruct")
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
