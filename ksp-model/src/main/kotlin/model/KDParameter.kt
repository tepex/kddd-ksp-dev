package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName

@ConsistentCopyVisibility
public data class KDParameter private constructor(
    val name: MemberName,
    public val typeReference: KDReference
) {

    public sealed interface KDReference {
        public val typeName: TypeName

        @JvmInline
        public value class Element private constructor(override val typeName: TypeName) : KDReference {

            override fun toString(): String =
                typeName.toString()

            public companion object {
                public fun create(typeName: TypeName): Element =
                    Element(typeName)
            }
        }

        @ConsistentCopyVisibility
        public data class Collection private constructor(
            val parameterizedTypeName: ParameterizedTypeName,
            val collectionType: CollectionType
        ) : KDReference {
            override val typeName: TypeName = parameterizedTypeName

            public enum class CollectionType(
                public val className: ClassName,
                public val initializer: String,
                public val mutableInitializer: String
            ) {
                SET(com.squareup.kotlinpoet.SET, "emptySet()", "mutableSetOf()"),
                LIST(com.squareup.kotlinpoet.LIST, "emptyList()", "mutableListOf()"),
                MAP(com.squareup.kotlinpoet.MAP, "emptyMap()", "mutableMapOf()");
            }

            public companion object {
                public fun create(typeName: ParameterizedTypeName): Collection = CollectionType.entries
                    .find { it.className == typeName.rawType }
                    ?.let { Collection(typeName, it) }
                    ?: error("Not supported collection type $typeName")
            }
        }

        public companion object {
            public fun create(typeName: TypeName): KDReference = when(typeName) {
                is ParameterizedTypeName -> Collection.create(typeName)
                else                     -> Element.create(typeName)
            }
        }
    }

    public companion object {
        public fun create(name: MemberName, propertyTypeName: TypeName): KDParameter =
            KDParameter(name, KDReference.create(propertyTypeName))

        public fun create(property: KDTypeHelper.Property): KDParameter =
            KDParameter(property.name, KDReference.create(property.typeName))
    }
}
