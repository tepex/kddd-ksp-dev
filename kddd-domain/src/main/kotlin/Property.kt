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
    val className: CompositeClassName,
    val isNullable: Boolean,
    val collectionType: CollectionType?
) : ValueObject.Data {

    private val asCode: String by lazy {
        StringBuilder(name.boxed).apply {
            if (serialName.boxed != name.boxed) append("""[serialName="${serialName}"]""")
            append(": $className")
            if (isNullable) append('?')
        }.toString()
    }

    init {
        validate()
    }

    override fun validate() {
        require((collectionType != null && isNullable).not()) { "Property '$name' is a Collection and can't be nullable!" }
    }

    /*
    override fun toString(): String =
        "[$asCode]"*/

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

    public enum class CollectionType {
        SET, LIST, MAP
    }

    public class Builder {
        public var name: Name? = null
        public var serialName: String? = null
        public var packageName: CompositeClassName.PackageName? = null
        public var className: String? = null
        public var isNullable: Boolean? = null
        public var collectionType: CollectionType? = null

        public fun build(): Property {
            checkNotNull(name) { "Property 'name' must be initialized!" }
            checkNotNull(packageName) { "Property 'pkgName' must be initialized!" }
            checkNotNull(className) { "Property 'className' must be initialized!" }
            checkNotNull(isNullable) { "Property 'isNullable' must be initialized!" }

            return Property(
                name!!,
                SerialName(serialName ?: name!!.boxed),
                compositeClassName {
                    packageName = this@Builder.packageName
                    fullClassName = className!!.replace("?", "")
                },
                isNullable!!,
                collectionType
            )
        }
    }
}
