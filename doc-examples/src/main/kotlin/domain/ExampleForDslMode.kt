package ru.it_arch.kddd.magic.domain

import ru.it_arch.kddd.ValueObject
import java.io.File
import java.util.UUID

/**
 * [qqqq](https://ya.ru/)
 * */
interface ExampleForDslMode : ValueObject.Data {
    val primitive: Primitive
    val anyUuid: CommonUuid
    val anyFile: CommonFile

    val nested: SomeNestedType
    val simpleList: List<Primitive>
    val simpleMap: Map<Primitive, CommonUuid?>
    //val complexCollection: Map<Set<Primitive>, Map<Primitive, CommonFile>>

    interface Primitive : ValueObject.Value<Int> {
        override fun validate() {}
    }

    // Parsable mode
    interface CommonUuid : ValueObject.Value<UUID> {
        override fun validate() {}
    }

    // As is mode
    interface CommonFile : ValueObject.Value<File> {
        override fun validate() {}
    }

    interface SomeNestedType : ValueObject.Data {
        val simple: SimpleType
        val nullableSimple: SimpleType?

        override fun validate() {}

        interface SimpleType : ValueObject.Value<String> {
            override fun validate() {}
        }
    }
}
