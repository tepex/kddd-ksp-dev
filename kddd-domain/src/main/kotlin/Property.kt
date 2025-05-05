package ru.it_arch.clean_ddd.domain

import ru.it_arch.kddd.KDSerialName
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

/**
 * Определяет свойство в классе имплементации.
 *
 * @property name текстовое имя свойства.
 * @property serialName имя сериализированного свойства если применена аннотация [KDSerialName].
 * @property className тип свойства.
 *
 * */
@ConsistentCopyVisibility
public data class Property private constructor(
    val name: Name,
    val serialName: SerialName,
    val className: ClassName,
    val isNullable: Boolean
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

    @JvmInline
    public value class ClassName private constructor(override val boxed: String) : ValueObject.Boxed<String> {
        override fun validate() {}

        override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T {
            TODO("Not yet implemented")
        }

        override fun toString(): String = boxed

        public companion object {
            public operator fun invoke(value: String): ClassName =
                ClassName(value)
        }
    }

    public class Builder {
        public var name: String? = null
        public var serialName: String? = null
        public var className: String? = null
        public var isNullable: Boolean? = null

        public fun build(): Property {
            checkNotNull(name) { "Property 'name' must be initialized!" }
            checkNotNull(className) { "Property 'className' must be initialized!" }
            checkNotNull(isNullable) { "Property 'isNullable' must be initialized!" }
            return Property(Name(name!!), SerialName(serialName ?: name!!), ClassName(className!!), isNullable!!)
        }
    }
}
