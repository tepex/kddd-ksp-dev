package ru.it_arch.clean_ddd.ksp.interop

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import ru.it_arch.ddd.ValueObjectSingle

internal sealed interface KDReference {
    val typeName: TypeName

    @JvmInline
    value class Element private constructor(override val value: TypeName) : ValueObjectSingle<TypeName>, KDReference {
        override val typeName: TypeName
            get() = value

        override fun validate() {}

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObjectSingle<TypeName>> copy(value: TypeName): T =
            create(value) as T

        override fun toString(): String =
            value.toString()

        companion object {
            fun create(typeName: TypeName) =
                Element(typeName)
        }
    }

    @ConsistentCopyVisibility
    data class Collection private constructor(
        val parameterizedTypeName: ParameterizedTypeName,
        val collectionType: CollectionType
    ) : KDReference {
        override val typeName: TypeName = parameterizedTypeName

        enum class CollectionType(val className: ClassName, val initializer: String, val mutableInitializer: String) {
            SET(com.squareup.kotlinpoet.SET, "emptySet()", "mutableSetOf()"),
            LIST(com.squareup.kotlinpoet.LIST, "emptyList()", "mutableListOf()"),
            MAP(com.squareup.kotlinpoet.MAP, "emptyMap()", "mutableMapOf()");
        }

        companion object {
            fun create(typeName: ParameterizedTypeName) = CollectionType.entries
                .find { it.className == typeName.rawType }
                ?.let { Collection(typeName, it) }
                ?: error("Not supported collection type $typeName")
        }
    }

    companion object {
        fun create(typeName: TypeName): KDReference = when(typeName) {
            is ParameterizedTypeName -> Collection.create(typeName)
            else -> Element.create(typeName)
        }
    }
}
