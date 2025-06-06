package ru.it_arch.kddd.magic.domain

import ru.it_arch.kddd.ValueObject
import java.io.File
import java.util.UUID

/**
 * [Регламент/Интерфейс CDT п.1](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-abstraction)
 * [Регламент/Интерфейс CDT п.2](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-kddd)
 * */
interface ExampleForDslMode : ValueObject.Data {
    /** [Регламент/Интерфейс CDT п.3](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-immutable) */
    val primitive: Primitive
    /** [Регламент/Интерфейс CDT п.5.1](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-property-cdt) */
    val anyUuid: CommonUuid
    val anyFile: CommonFile

    val nested: SomeNestedType
    /** [Регламент/Интерфейс CDT п.5.1](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-property-cdt) */
    val simpleList: List<Primitive>
    val simpleMap: Map<Primitive, CommonUuid?>
    /** [Регламент/Интерфейс CDT п.5.2](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-property-collection) */
    //val complexCollection: Map<Set<Primitive>, Map<Primitive, CommonFile>>

    /** [Регламент/Интерфейс CDT п.7](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-validatable) */
    override fun validate() {}

    /** [Регламент/Интерфейс CDT п.5.3](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-property-nested)
     * [Регламент/Интерфейс CDT п.6.1](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-property-boxed)
     * */
    interface Primitive : ValueObject.Value<Int> {
        override fun validate() {}
    }

    /** [Регламент/Интерфейс CDT п.6.1](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-property-boxed) */
    interface CommonUuid : ValueObject.Value<UUID> {
        override fun validate() {}
    }

    /** [Регламент/Интерфейс CDT п.6.1](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-property-boxed) */
    interface CommonFile : ValueObject.Value<File> {
        override fun validate() {}
    }

    /** [Регламент/Интерфейс CDT п.5.3](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-property-nested) */
    interface SomeNestedType : ValueObject.Data {
        val simple: SimpleType
        /** [Регламент/Интерфейс CDT п.4](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-nullable) */
        val nullableSimple: SimpleType?

        override fun validate() {}

        interface SimpleType : ValueObject.Value<String> {
            override fun validate() {}
        }
    }
}
