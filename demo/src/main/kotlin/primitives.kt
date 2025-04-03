package ru.it_arch.clean_ddd.app

import ru.it_arch.clean_ddd.domain.demo.Primitives
import ru.it_arch.clean_ddd.domain.demo.impl.primitives

const val jsonSrc = """
{
    "str": "some string",
    "int": 44
}"""

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

fun testPrimitives() {

    val demo = primitives {
        str = "some string for demo"
        size = 55
    }

    println("demo: $demo")

    val jsonStr = json.encodeToString(demo)
    println("json: $jsonStr")

    val obj = json.decodeFromString<Primitives>(jsonSrc)
    println("deserialize: $obj")

}
