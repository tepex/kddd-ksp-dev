package ru.it_arch.kddd.domain.model

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
    val type: PropertyType,
    //val className: CompositeClassName.FullClassName,
    val isNullable: Boolean
) : ValueObject.Data {

    /*
    private val asCode: String by lazy {
        StringBuilder(name.boxed).apply {
            if (serialName.boxed != name.boxed) append("""[serialName="${serialName}"]""")
            append(": $className")
            if (isNullable) append('?')
        }.toString()
    }*/

    init {
        validate()
    }

    override fun validate() {
        require((type is PropertyType.PropertyCollection && isNullable).not()) { "Property '$name' is a Collection and can't be nullable!" }
    }

    /*
    override fun toString(): String =
        "[$asCode]"*/

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T =
        TODO("Must not used")

    @JvmInline
    public value class Name(override val boxed: String): ValueObject.Value<String> {
        init {
            validate()
        }

        override fun validate() {}

        override fun toString(): String =
            boxed

        override fun <T : ValueObject.Value<String>> fork(boxed: String): T =
            TODO("Must not used")
    }

    @JvmInline
    public value class SerialName(override val boxed: String): ValueObject.Value<String> {
        init {
            validate()
        }

        override fun validate() {}

        override fun toString(): String =
            boxed

        override fun <T : ValueObject.Value<String>> fork(boxed: String): T =
            TODO("Must not used")
    }

    public sealed interface PropertyType {
        @JvmInline
        public value class PropertyElement(
            override val boxed: CompositeClassName.FullClassName
        ): ValueObject.Value<CompositeClassName.FullClassName>, PropertyType {
            override fun <T : ValueObject.Value<CompositeClassName.FullClassName>> fork(boxed: CompositeClassName.FullClassName): T {
                TODO("Not yet implemented")
            }

            override fun validate() {}
        }

        public sealed interface PropertyCollection : PropertyType {
            @JvmInline
            public value class PropertySet(override val boxed: PropertyType) : ValueObject.Value<PropertyType>, PropertyCollection {

                override fun validate() {}
                override fun <T : ValueObject.Value<PropertyType>> fork(boxed: PropertyType): T {
                    TODO("Not yet implemented")
                }
            }

            @JvmInline
            public value class PropertyList(override val boxed: PropertyType) : ValueObject.Value<PropertyType>, PropertyCollection {
                override fun validate() {}

                override fun <T : ValueObject.Value<PropertyType>> fork(boxed: PropertyType): T {
                    TODO("Not yet implemented")
                }
            }

            public data class PropertyMap(
                val first: PropertyType,
                val second: PropertyType
            ) : ValueObject.Data, PropertyCollection {
                override fun validate() {}

                override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
                    TODO("Not yet implemented")
                }
            }
        }
    }

    public class Builder {
        public var name: Name? = null
        public var serialName: String? = null
        public var type: PropertyType? = null
        //public var className: CompositeClassName.FullClassName? = null
        public var isNullable: Boolean? = null

        public fun build(): Property {
            checkNotNull(name) { "Property 'name' must be initialized!" }
            checkNotNull(type) { "Property 'type' must be initialized!" }
            checkNotNull(isNullable) { "Property 'isNullable' must be initialized!" }

            return Property(
                name!!,
                SerialName(serialName ?: name!!.boxed),
                type!!,
                isNullable!!,
            )
        }
    }
}
