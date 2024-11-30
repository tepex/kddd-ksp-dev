package ru.it_arch.clean_ddd.ksp.interop

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import ru.it_arch.ddd.ValueObject
import ru.it_arch.ddd.ValueObjectSingle

internal sealed interface KDReference {
    val typeName: TypeName

    @JvmInline
    value class Element private constructor(override val value: TypeName) : ValueObjectSingle<TypeName>, KDReference {
        override val typeName: TypeName
            get() = value

        override fun validate() {}

        companion object {
            fun create(typeName: TypeName) =
                Element(typeName)
        }
    }

    @ConsistentCopyVisibility
    data class Collection private constructor(
        val parameterizedTypeName: ParameterizedTypeName,
        val collectionType: CollectionType
    ) : ValueObject, KDReference {
        override val typeName: TypeName = parameterizedTypeName

        override fun validate() {}

        enum class CollectionType(val initializer: String) {
            SET("emptySet()"), LIST("emptyList()"), MAP("emptyMap()")
        }

        companion object {
            fun create(parameterizedTypeName: ParameterizedTypeName, collectionType: CollectionType) =
                Collection(parameterizedTypeName, collectionType)
        }
    }

    companion object {
        fun create(typeName: TypeName): KDReference = when(typeName) {
            is ParameterizedTypeName -> Collection.CollectionType.entries
                .find { it.name.lowercase().replaceFirstChar { it.titlecaseChar() } == typeName.rawType.simpleName }
                ?.let { Collection.create(typeName, it) }
                ?: error("Not supported collection type $typeName")
            else -> Element.create(typeName)
        }
    }
}
