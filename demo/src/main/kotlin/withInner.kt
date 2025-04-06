package ru.it_arch.clean_ddd.app

import ru.it_arch.clean_ddd.domain.WithInnerCustomImpl
import ru.it_arch.clean_ddd.domain.demo.WithInner
import ru.it_arch.clean_ddd.domain.withInnerCustom

fun testWithInner() {
    withInnerCustom {
        myInner = myInner {
            innerLong = 22
            innerStr = "some string"
        }
    }.apply {
        println("\ninner types demo: $this")
        //json.encodeToString(this).also { println("json: $it") }
    }

    val jsonSrc = """
{
    "my-inner": {
        "inner-long": 33,
        "inner-str": "sdfsdfsxc"
    },
    "my-optional-inner": {
        "inner-long": 11,
        "inner-str": "optional string"
    }
}""".apply {
        json.decodeFromString<WithInnerCustomImpl>(this).also { obj: WithInner ->
            // Можете пользоваться
            println("deserialize: $obj")
        }
    }
}
