package ru.it_arch.kddd.magic

import io.kotest.matchers.maps.shouldMatchAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
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
            simpleList += listOf(11, 22, 33)
            simpleMap[10] = "3aefca38-8edd-4121-9ab4-89bf5c6ba84b"
            simpleMap[20] = "478bd6e6-488b-45ea-9a27-4bcaf937a8e4"
            simpleMap[30] = "8b189d40-7624-454b-a85b-9b6aacb9f097"
        }
    }.onSuccess { example ->
        println("origin: $example")
        example.toDslBuilder().apply {
            primitive = 44
            simpleMap[20] = null
            simpleMap[30] = TEST_UUID
        }.build().also { changed ->
            println("changed: $changed")
            println("new primitive: ${changed.primitive.boxed shouldBe 44}")
            changed.simpleMap.shouldMatchAll(
                ExampleForDslModeImpl.PrimitiveImpl(20) to { uuid -> uuid.shouldBeNull() },
                ExampleForDslModeImpl.PrimitiveImpl(30) to
                    { uuid -> uuid shouldBe ExampleForDslModeImpl.CommonUuidImpl(UUID.fromString(TEST_UUID)) }
            )
        }
    }
}
