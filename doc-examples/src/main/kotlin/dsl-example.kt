package ru.it_arch.kddd.magic

import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldMatchAll
import io.kotest.matchers.maps.shouldMatchExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ru.it_arch.kddd.magic.domain.ExampleForDslMode
import ru.it_arch.kddd.magic.impl.ExampleForDslModeImpl
import ru.it_arch.kddd.magic.impl.exampleForDslMode
import ru.it_arch.kddd.magic.impl.someNestedType
import ru.it_arch.kddd.magic.impl.toDslBuilder
import java.io.File
import java.util.UUID

const val TEST_UUID = "50d3d60b-b4d7-4fca-a984-d911a3688f99"

fun dslExample() {

    runCatching {
        exampleForDslMode {
            primitive = 33
            anyUuid = TEST_UUID
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
            simpleMap[2] = null
            simpleMap[3] = TEST_UUID
        }.build().also { changed ->
            println("changed: $changed")
            println("new primitive: ${changed.primitive.boxed shouldBe 44}")
            changed.simpleMap.shouldMatchAll(
                ExampleForDslModeImpl.PrimitiveImpl(2) to { it.shouldBeNull() },
                ExampleForDslModeImpl.PrimitiveImpl(3) to { it shouldBe ExampleForDslModeImpl.CommonUuidImpl(UUID.fromString(TEST_UUID)) }
            )
        }
    }
}
