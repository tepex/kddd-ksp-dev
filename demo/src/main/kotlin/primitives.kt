package ru.it_arch.clean_ddd.app

import ru.it_arch.clean_ddd.domain.MySimpleJson
import ru.it_arch.clean_ddd.domain.mySimpleJson
import java.util.UUID

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

fun primitives() {
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

}
