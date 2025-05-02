package ru.it_arch.clean_ddd.domain

import ru.it_arch.kddd.KDSerialName
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

/**
 * Определяет свойство в классе имплементации.
 *
 * @property name текстовое имя свойства.
 * @property serialName имя сериализированного свойства если применена аннотация [KDSerialName].
 * @property type тип свойства.
 *
 * */
@ConsistentCopyVisibility
public data class Property private constructor(
    val name: Name,
    val serialName: SerialName,
    val type: ClassName,
) : ValueObject.Data {

    init {
        validate()
    }

    override fun validate() {}

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T =
        TODO("Must not used")

    @JvmInline
    public value class Name(override val boxed: String): ValueObject.Boxed<String> {
        init {
            validate()
        }

        override fun validate() {}

        override fun toString(): String =
            boxed

        override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T =
            TODO("Must not used")
    }

    @JvmInline
    public value class SerialName(override val boxed: String): ValueObject.Boxed<String> {
        init {
            validate()
        }

        override fun validate() {}

        override fun toString(): String =
            boxed

        override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T =
            TODO("Must not used")
    }

    public class Builder {
        public var name: String? = null
        public var serialName: String? = null
        public var type: ClassName? = null

        public fun build(): Property {
            requireNotNull(name) { "Property 'name' must be initialized!" }
            requireNotNull(serialName) { "Property 'serialName' must be initialized!" }
            requireNotNull(type) { "Property 'type' must be initialized!" }
            return Property(Name(name!!), SerialName(serialName!!), type!!)
        }
    }
}
