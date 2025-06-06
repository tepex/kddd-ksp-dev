package ru.it_arch.kddd.magic

import ru.it_arch.kddd.magic.impl.exampleForDslMode
import ru.it_arch.kddd.magic.impl.someNestedType
import ru.it_arch.kddd.magic.impl.toDslBuilder
import java.io.File
import java.util.UUID

fun dslExample() {

    runCatching {
        exampleForDslMode {
            primitive = 33
            anyUuid = "50d3d60b-b4d7-4fca-a984-d911a3688f99"
            anyFile = File("some-file")
            nested = someNestedType {
                simple = "I'm simple"
            }
            simpleList += listOf(1, 2, 3)
            simpleMap += mapOf(
                1 to UUID.randomUUID().toString(),
                2 to UUID.randomUUID().toString(),
                3 to UUID.randomUUID().toString()
            )
        }
    }.onSuccess { example ->
        println("origin: $example")
        example.toDslBuilder().apply {
            primitive = 44
            simpleMap[3] = "50d3d60b-b4d7-4fca-a984-d911a3688f99"
        }.build()
            .also { println("changed: $it") }
        //assertTh
    }
}
