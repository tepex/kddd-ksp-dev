package ru.it_arch.clean_ddd.app

import ru.it_arch.clean_ddd.domain.demo.impl.toJson
import ru.it_arch.clean_ddd.domain.demo.impl.toWithInner
import ru.it_arch.clean_ddd.domain.demo.impl.withInner

fun testWithInner() {
    withInner {
        myInner = myInner {
            innerLong = 22
            innerStr = "some string"
        }
    }.apply {
        println("\ninner types demo: $this")
        println("json: ${toJson()}")
    }

    """{
    "my-inner": {
        "inner-long": 33,
        "inner-str": "sdfsdfsxc"
    },
    "my-optional-inner": {
        "inner-long": 11,
        "inner-str": "optional string"
    }
}""".apply {
        println("deserialize: ${toWithInner()}")
    }
}
